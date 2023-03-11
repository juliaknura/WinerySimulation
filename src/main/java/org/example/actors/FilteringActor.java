package org.example.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.msg.*;

import java.util.ArrayList;

import static java.lang.Thread.sleep;

public class FilteringActor extends AbstractBehavior<Msg> {

    private int unfilteredWineAmount;
    private final int UNFILTERED_WINE_AMOUNT_NEEDED=25;
    private final int PRODUCT_AMOUNT=24;
    private final int TIME = 43200000; //12 hours
    private final int SLOTS=10;
    private int usedUpSlots;
    private int speedUpFactor;
    private ActorRef<Msg> storage,bottling;
    private ArrayList<ActorRef<Msg>> children;
    private boolean fermentationFinished;

    private FilteringActor(ActorContext<Msg> context,ActorRef<Msg> storage, int speedUpFactor, ActorRef<Msg> bottling) {
        super(context);
        System.out.println("Setting up filtering stage");
        this.storage=storage;
        this.speedUpFactor=speedUpFactor;
        this.bottling=bottling;
        unfilteredWineAmount=0;
        fermentationFinished=false;
        usedUpSlots=0;
        children = new ArrayList<ActorRef<Msg>>();
        for(int i=0;i<SLOTS;i++)
        {
            children.add(context.spawn(FilteringChildActor.create(context.getSelf(),(TIME/speedUpFactor)),("child"+i)));
        }

    }

    public static Behavior<Msg> create(ActorRef<Msg> storage, int speedUpFactor,ActorRef<Msg> bottling) {
        return Behaviors.setup(context->new FilteringActor(context,storage,speedUpFactor,bottling));
    }

    @Override
    public Receive<Msg> createReceive() {
        return newReceiveBuilder()
                .onMessage(FilteredWine.class,this::msgFromChild)
                .onMessage(UnfilteredWine.class,this::addSupply)
                .onMessage(FermentationFinished.class,this::setFermentationFinished)
                .build();
    }

    private Behavior<Msg> msgFromChild(Msg msg) {
        usedUpSlots--;
        System.out.println("Produced "+PRODUCT_AMOUNT+" litres of filtered wine");
        bottling.tell(new FilteredWine(PRODUCT_AMOUNT));
        make();
        return this;
    }

    private Behavior<Msg> addSupply(Msg msg){
        if(msg instanceof UnfilteredWine)
        {
            unfilteredWineAmount+=((UnfilteredWine) msg).getAmount();
            System.out.println("Filtering stage: received unfiltered wine supply of "+((UnfilteredWine) msg).getAmount()+" litres");
        }
        make();
        return this;
    }

    private boolean enoughSlots()
    {
        if(usedUpSlots>=SLOTS)
        {
            return false;
        }
        else return true;
    }

    private void make() {
        while(unfilteredWineAmount>=UNFILTERED_WINE_AMOUNT_NEEDED)
        {
            if(enoughSlots())
            {
                System.out.println("Filtering of wine started");
                System.out.println("Using "+UNFILTERED_WINE_AMOUNT_NEEDED+" amount of unfiltered wine in slot number "+usedUpSlots);
                children.get(usedUpSlots).tell(new StartChildMsg());
                usedUpSlots++;
                unfilteredWineAmount-=UNFILTERED_WINE_AMOUNT_NEEDED;
            }
            else {
                return;
            }
        }
        if(fermentationFinished && usedUpSlots==0)
        {
            System.out.println("Filtering stage finished production");
            storage.tell(new FilteringFinished());
            bottling.tell(new FilteringFinished());
        }
    }

    private Behavior<Msg> setFermentationFinished(Msg msg){
        fermentationFinished = true;
        make();
        return this;
    }


}
