package org.example.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.msg.*;

public class StorageActor extends AbstractBehavior<Msg> {

    private final ActorRef<Msg> pressing, fermentation,filtering,bottling;
    private int grapesAmount, waterAmount, sugarAmount, bottlesAmount, finishedProductAmount, speedUpFactor;
    private boolean[] areFinished;
    private final int NUMBER_OF_STAGES=4;

    public static Behavior<Msg> create(int grapesAmount, int waterAmount, int sugarAmount, int bottlesAmount, int speedUpFactor)
    {
        System.out.println("storage actor is created");
        return Behaviors.setup(context -> new StorageActor(context,grapesAmount, waterAmount, sugarAmount, bottlesAmount,speedUpFactor));
    }


    private StorageActor(ActorContext<Msg> context, int grapesAmount, int waterAmount, int sugarAmount, int bottlesAmount, int speedUpFactor) {
        super(context);
        System.out.println("Setting up the storage");
        bottling = context.spawn(BottlingActor.create(context.getSelf(),speedUpFactor),"bottling");
        filtering = context.spawn(FilteringActor.create(context.getSelf(),speedUpFactor,bottling),"filtering");
        fermentation = context.spawn(FermentationActor.create(context.getSelf(),speedUpFactor,filtering),"fermentation");
        pressing = context.spawn(PressingActor.create(context.getSelf(),speedUpFactor,fermentation),"pressing");
        this.grapesAmount = grapesAmount;
        this.waterAmount = waterAmount;
        this.sugarAmount = sugarAmount;
        this.bottlesAmount = bottlesAmount;
        finishedProductAmount = 0;
        this.speedUpFactor=speedUpFactor;
        areFinished = new boolean[]{false, false, false, false};
    }

    @Override
    public Receive<Msg> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartMsg.class,this::startProduction)
                .onMessage(FinishedProduct.class,this::addSupplies)
                .onMessage(PressingFinished.class,this::setFinished)
                .onMessage(FermentationFinished.class,this::setFinished)
                .onMessage(FilteringFinished.class,this::setFinished)
                .onMessage(BottlingFinished.class,this::setFinished)
                .onMessage(StorageFinished.class,this::thatsAllFolks)
                .build();
    }

    private Behavior<Msg> startProduction(Msg msg) {
        System.out.println("Start of Production");
        pressing.tell(new Grapes(grapesAmount));
        grapesAmount=0;
        fermentation.tell(new Water(waterAmount));
        waterAmount=0;
        fermentation.tell(new Sugar(sugarAmount));
        sugarAmount=0;
        bottling.tell(new Bottles(bottlesAmount));
        bottlesAmount=0;
        return this;
    }

    private Behavior<Msg> addSupplies(FinishedProduct msg)
    {
        if(msg instanceof FinishedProduct)
        {
            finishedProductAmount+=msg.getAmount();
            System.out.println("Storage: received a supply of wine bottles in amount of: "+msg.getAmount());
        }
        return this;
    }

    private Behavior<Msg> setFinished(Msg msg)
    {
        if(msg instanceof PressingFinished)
        {
            areFinished[0]=true;
        }
        if(msg instanceof FermentationFinished)
        {
            areFinished[1]=true;
        }
        if(msg instanceof FilteringFinished)
        {
            areFinished[2]=true;
        }
        if(msg instanceof BottlingFinished)
        {
            areFinished[3]=true;
        }
        checkIfAllFinished();
        return this;
    }

    private void checkIfAllFinished()
    {
        for(int i=0;i<NUMBER_OF_STAGES;i++)
        {
            if(areFinished[i]==false)
            {
                return;
            }
        }
        getContext().getSelf().tell(new StorageFinished());
    }
    private Behavior<Msg> thatsAllFolks(Msg msg)
    {
        System.out.println("Final amount of produced bottles of wine: "+finishedProductAmount);
        System.out.println("Enjoy!");
        return Behaviors.stopped();
    }
}
