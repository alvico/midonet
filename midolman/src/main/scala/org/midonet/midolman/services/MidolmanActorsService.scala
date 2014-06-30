/*
 * Copyright (c) 2013 Midokura SARL, All Rights Reserved.
 */
package org.midonet.midolman.services

import java.util.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.reflect.ClassTag
import scala.util.Try

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.{gracefulStop, ask}
import akka.util.Timeout
import com.google.common.util.concurrent.AbstractService
import com.google.inject.{Inject, Injector}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import org.midonet.midolman.DatapathController
import org.midonet.midolman.FlowController
import org.midonet.midolman.NetlinkCallbackDispatcher
import org.midonet.midolman.PacketsEntryPoint
import org.midonet.midolman.SupervisorActor
import org.midonet.midolman.config.MidolmanConfig
import org.midonet.midolman.l4lb.HealthMonitor
import org.midonet.midolman.monitoring.MonitoringActor
import org.midonet.midolman.routingprotocols.RoutingManagerActor
import org.midonet.midolman.topology.VirtualToPhysicalMapper
import org.midonet.midolman.topology.VirtualTopologyActor

/*
 * A base trait for a simple guice service that starts an actor system,
 * a SupervisorActor and set of top-level actors below the supervisor.
 *
 * Concrete classes need to override actorSpecs, to select which top-level
 * actors this service should provide.
 */
class MidolmanActorsService extends AbstractService {

    import SupervisorActor.StartChild

    private val log = LoggerFactory.getLogger(classOf[MidolmanActorsService])

    @Inject
    val injector: Injector = null
    @Inject
    val config: MidolmanConfig = null

    private var _system: ActorSystem = null
    implicit def system: ActorSystem = _system
    implicit def ex: ExecutionContext = _system.dispatcher
    implicit protected val tout = new Timeout(5 seconds)

    protected def actorSpecs = {
        val specs = List(
            (propsFor(classOf[VirtualTopologyActor]),      VirtualTopologyActor.Name),
            (propsFor(classOf[VirtualToPhysicalMapper]).
                withDispatcher("actors.stash-dispatcher"), VirtualToPhysicalMapper.Name),
            (propsFor(classOf[DatapathController]),        DatapathController.Name),
            (propsFor(classOf[FlowController]),            FlowController.Name),
            (propsFor(classOf[RoutingManagerActor]),       RoutingManagerActor.Name),
            (propsFor(classOf[HealthMonitor]).
                withDispatcher("actors.pinned-dispatcher"),HealthMonitor.Name),
            (propsFor(classOf[PacketsEntryPoint]),       PacketsEntryPoint.Name),
            (propsFor(classOf[NetlinkCallbackDispatcher]), NetlinkCallbackDispatcher.Name))

        if (config.getMidolmanEnableMonitoring)
            (propsFor(classOf[MonitoringActor]), MonitoringActor.Name) :: specs
        else
            specs
    }

    protected var supervisorActor: ActorRef = _
    private var childrenActors: List[ActorRef] = Nil

    protected override def doStart() {
        try {
            log.info("Booting up actors service")

            _system = createActorSystem()

            supervisorActor = startTopActor(
                                propsFor(classOf[SupervisorActor]),
                                SupervisorActor.Name)

            childrenActors = actorSpecs map {
                s =>
                    try {
                        Await.result(startActor(s), tout.duration)
                    } catch {
                        case t: Throwable =>
                            // rethrow and propagate up to the injector
                            throw new Exception("$s._2 creation failed", t)
                    }
            }

            notifyStarted()
            log.info("Actors system started")
        } catch {
            case e: Throwable =>
                log.error("Error while starting midolman actors", e)
                notifyFailed(e)
        }
    }

    protected override def doStop() {
        try {
            val stopFutures = childrenActors map { child => stopActor(child) }
            try {
                log.debug("Stopping the actor system")
                Await.result(Future.sequence(stopFutures), 500 millis)
                stopActor(supervisorActor)
            } catch {
                case e: TimeoutException =>
                    log.warn("Failed to gracefully stop the actor system")
            }
            system.shutdown()
            system.awaitTermination()
            // Because Akka sucks...
            system.dispatchers
                  .defaultGlobalDispatcher
                  .asInstanceOf[{def shutdown(): Unit}]
                  .shutdown()
            notifyStopped()
        } catch {
            case e: Throwable =>
                log.error("Exception", e)
                notifyFailed(e)
        }
    }

    def propsFor[T <: Actor: ClassTag](actorClass: Class[T]) =
        Props { injector.getInstance(actorClass) }

    protected def stopActor(actorRef: ActorRef) = {
        log.debug("Stopping actor: {}", actorRef.toString())
        gracefulStop(actorRef, 100.millis)
    }

    private def startTopActor(actorProps: Props, actorName: String) = {
        log.debug("Starting actor {}", actorName)
        system.actorOf(actorProps, actorName)
    }

    protected def startActor(specs: (Props, String)): Future[ActorRef] = {
        val (props, name) = specs
        log.debug("Request for starting actor {}", name)
        (supervisorActor ? StartChild(props, name)).mapTo[ActorRef]
    }

    protected def createActorSystem(): ActorSystem =
        ActorSystem.create("MidolmanActors", ConfigFactory.load()
                .getConfig("midolman"))

    def initProcessing() {
        log.debug("Sending Initialization message to datapath controller.")
        DatapathController ! DatapathController.initializeMsg
    }
}
