package org.example.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.Main;
import org.example.msg.Msg;
import org.example.msg.StartChildMsg;
import org.example.msg.UnfilteredWine;

import static java.lang.Thread.sleep;

public class FermentationChildActor extends AbstractBehavior<Msg> {

    private ActorRef<Msg> parent;
    private final int PRODUCT_AMOUNT=25;
    private final double PROBABILITY_OF_FAILURE = 0.05;
    private int time;

    public static Behavior<Msg> create(ActorRef<Msg> parent, int time)
    {
        return Behaviors.setup(context->new FermentationChildActor(context,parent,time));
    }

    private FermentationChildActor(ActorContext<Msg> context, ActorRef<Msg> parent, int time) {
        super(context);
        //System.out.println("dziecko sie tworzy");
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
        //System.out.println("Wiadomosc dotarla do dziecka");
        sleep(time);
        double succeed = Math.random();
        if(succeed<PROBABILITY_OF_FAILURE)
        {
            parent.tell(new UnfilteredWine(0));
        }
        else
        {
            parent.tell(new UnfilteredWine(PRODUCT_AMOUNT));
        }
        return this;
    }
}
