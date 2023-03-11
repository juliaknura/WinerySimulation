package org.example.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.msg.*;

import static java.lang.Thread.sleep;

public class BottlingActor extends AbstractBehavior<Msg> {

    private double filteredWineAmount;
    private int bottlesAmount;
    private final double FILTERED_WINE_AMOUNT_NEEDED = 0.75;
    private final double BOTTLES_AMOUNT_NEEDED = 1;
    private final int PRODUCT_AMOUNT=1;
    private final double PROBABILITY_OF_FAILURE = 0.05;
    private final int TIME = 300000; //5 minutes
    private final int SLOTS=1;
    private int speedUpFactor;
    private ActorRef<Msg> storage;
    private boolean filteringFinished;

    private BottlingActor(ActorContext<Msg> context, ActorRef<Msg> storage, int speedUpFactor) {
        super(context);
        System.out.println("Setting up bottling stage");
        this.storage=storage;
        this.speedUpFactor=speedUpFactor;
        filteredWineAmount=0;
        bottlesAmount=0;
        filteringFinished=false;
    }

    public static Behavior<Msg> create(ActorRef<Msg> storage,int speedUpFactor) {
        return Behaviors.setup(context -> new BottlingActor(context,storage,speedUpFactor));
    }

    @Override
    public Receive<Msg> createReceive() {
        return newReceiveBuilder()
                .onMessage(FilteredWine.class,this::addSupply)
                .onMessage(Bottles.class,this::addSupply)
                .onMessage(FilteringFinished.class,this::setFilteringFinished)
                .build();
    }

    private Behavior<Msg> addSupply(Msg msg) throws InterruptedException {

        if(msg instanceof FilteredWine)
        {
            filteredWineAmount+=((FilteredWine) msg).getAmount();
            System.out.println("Bottling stage: received filtered wine supply of "+((FilteredWine) msg).getAmount()+ " litres");
        }
        else if(msg instanceof Bottles)
        {
            bottlesAmount+= ((Bottles) msg).getAmount();
            System.out.println("Bottling stage: received bottle supply of "+((Bottles) msg).getAmount()+" bottles");
        }
        make();
        return this;
    }

    private boolean enoughSupplies()
    {
        if(bottlesAmount<BOTTLES_AMOUNT_NEEDED) return false;
        if(filteredWineAmount<FILTERED_WINE_AMOUNT_NEEDED) return false;
        return true;
    }

    private void make() throws InterruptedException {
        while(enoughSupplies())
        {
            System.out.println("Bottling started");
            bottlesAmount-=BOTTLES_AMOUNT_NEEDED;
            filteredWineAmount-=FILTERED_WINE_AMOUNT_NEEDED;
            System.out.println("Using "+BOTTLES_AMOUNT_NEEDED+" amount of bottles and "+
                            FILTERED_WINE_AMOUNT_NEEDED+" amount of filtered wine in slot number 1"
                    );
            sleep(TIME/speedUpFactor);
            double succeded = Math.random();
            if(succeded<PROBABILITY_OF_FAILURE)
            {
                System.out.println("Bottling failed");
            }
            else
            {
                System.out.println("Produced: "+PRODUCT_AMOUNT+" bottle of wine");
                storage.tell(new FinishedProduct(PRODUCT_AMOUNT));
            }
        }
        if(filteringFinished)
        {
            System.out.println("Bottling stage finished production");
            storage.tell(new BottlingFinished());
        }

    }

    private Behavior<Msg> setFilteringFinished(Msg msg) throws InterruptedException {
        filteringFinished=true;
        make();
        return this;
    }
}
