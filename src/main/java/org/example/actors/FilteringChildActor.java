package org.example.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.msg.FilteredWine;
import org.example.msg.Msg;
import org.example.msg.StartChildMsg;

import static java.lang.Thread.sleep;

public class FilteringChildActor extends AbstractBehavior<Msg> {

    private ActorRef<Msg> parent;
    private final int PRODUCT_AMOUNT=24;
    private int time;

    public static Behavior<Msg> create(ActorRef<Msg> parent, int time)
    {
        return Behaviors.setup(context->new FilteringChildActor(context,parent,time));
    }


    private FilteringChildActor(ActorContext<Msg> context,ActorRef<Msg> parent, int time) {
        super(context);
        this.parent=parent;
        this.time=time;
    }

    @Override
    public Receive<Msg> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartChildMsg.class,this::make)
                .build();
    }

    private Behavior<Msg> make(Msg msg) throws InterruptedException {
        sleep(time);
        parent.tell(new FilteredWine(PRODUCT_AMOUNT));
        return this;
    }
}
