package org.example.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.msg.Grapes;
import org.example.msg.Juice;
import org.example.msg.Msg;
import org.example.msg.PressingFinished;

import static java.lang.Thread.sleep;

public class PressingActor extends AbstractBehavior<Msg> {

    private int grapesAmount;
    private final int GRAPES_AMOUNT_NEEDED=15;
    private final int PRODUCT_AMOUNT=10;
    private final int TIME = 43200000; //12 hours
    private final int SLOTS=1;
    private int speedUpFactor;
    private ActorRef<Msg> storage, fermentation;

    public PressingActor(ActorContext<Msg> context,ActorRef<Msg> storage, int speedUpFactor, ActorRef<Msg> fermentation) {
        super(context);
        System.out.println("Setting up pressing stage");
        grapesAmount=0;
        this.storage=storage;
        this.speedUpFactor=speedUpFactor;
        this.fermentation=fermentation;
    }

    public static Behavior<Msg> create(ActorRef<Msg> storage,int speedUpFactor,ActorRef<Msg> fermentation) {
        return Behaviors.setup(context->new PressingActor(context,storage,speedUpFactor,fermentation));
    }

    @Override
    public Receive<Msg> createReceive() {
        return newReceiveBuilder()
                .onMessage(Grapes.class,this::addSupply)
                .build();
    }

    private Behavior<Msg> addSupply(Msg msg) {
        if(msg instanceof Grapes)
        {
            grapesAmount+=((Grapes) msg).getAmount();
            System.out.println("Pressing stage: received grape supply of "+((Grapes) msg).getAmount()+" kg");
        }
        try {
            make();
        } catch (InterruptedException e) {
        }
        return this;
    }

    private void make() throws InterruptedException {

        while(grapesAmount>=GRAPES_AMOUNT_NEEDED)
        {
            System.out.println("Production of juice started");
            grapesAmount-=GRAPES_AMOUNT_NEEDED;
            System.out.println("Using "+GRAPES_AMOUNT_NEEDED+" of grapes in slot number 1");
            sleep(TIME/speedUpFactor);
            System.out.println("Produced: "+PRODUCT_AMOUNT+" litres of juice");
            fermentation.tell(new Juice(PRODUCT_AMOUNT));
        }
        System.out.println("Pressing stage finished production");
        storage.tell(new PressingFinished());
        fermentation.tell(new PressingFinished());
    }
}
