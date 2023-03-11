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

public class FermentationActor extends AbstractBehavior<Msg> {

    private int sugarAmount;
    private int waterAmount;
    private int juiceAmount;
    private final int SUGAR_AMOUNT_NEEDED = 2;
    private final int WATER_AMOUNT_NEEDED = 8;
    private final int JUICE_AMOUNT_NEEDED = 15;
    private final int PRODUCT_AMOUNT=25;
    private final double PROBABILITY_OF_FAILURE = 0.05;
    private final int TIME = 1209600000; //14 days
    private final int SLOTS=10;
    private int usedUpSlots;
    private int speedUpFactor;
    private ActorRef<Msg> storage,filtering;
    private ArrayList<ActorRef<Msg>> children;
    private boolean pressingFinished;

    public FermentationActor(ActorContext<Msg> context,ActorRef<Msg> storage, int speedUpFactor,ActorRef<Msg> filtering) {
        super(context);
        System.out.println("Setting up fermentation stage");
        this.storage=storage;
        this.speedUpFactor=speedUpFactor;
        this.filtering=filtering;
        sugarAmount=0;
        waterAmount=0;
        juiceAmount=0;
        pressingFinished=false;
        usedUpSlots=0;
        children = new ArrayList<ActorRef<Msg>>();
        for(int i=0;i<SLOTS;i++)
        {
            children.add(context.spawn(FermentationChildActor.create(context.getSelf(),(TIME/speedUpFactor)),("child"+i)));
        }
    }

    public static Behavior<Msg> create(ActorRef<Msg> storage, int speedUpFactor,ActorRef<Msg> filtering) {
        return Behaviors.setup(context -> new FermentationActor(context,storage,speedUpFactor,filtering));
    }

    @Override
    public Receive<Msg> createReceive() {
        return newReceiveBuilder()
                .onMessage(UnfilteredWine.class,this::msgFromChild)
                .onMessage(Juice.class,this::addSupply)
                .onMessage(Water.class,this::addSupply)
                .onMessage(Sugar.class,this::addSupply)
                .onMessage(PressingFinished.class,this::setPressingFinished)
                .build();
    }

    private Behavior<Msg> msgFromChild(Msg msg) throws InterruptedException {
        //System.out.println("received msg from child");
        usedUpSlots--;
        int receivedProduct = ((UnfilteredWine)msg).getAmount();
        if(receivedProduct!=0)
        {
            System.out.println("Produced: "+PRODUCT_AMOUNT+" litres of unfiltered wine");
            filtering.tell(new UnfilteredWine(PRODUCT_AMOUNT));
        }
        else
        {
            System.out.println("Fermentation failed");
        }
        make();
        return this;
    }

    private Behavior<Msg> addSupply(Msg msg) throws InterruptedException {
        if(msg instanceof Juice)
        {
            juiceAmount+= ((Juice) msg).getAmount();
            System.out.println("Fermentation stage: received juice supply of "+((Juice) msg).getAmount()+" litres");
        }
        else if(msg instanceof Water)
        {
            waterAmount+= ((Water) msg).getAmount();
            System.out.println("Fermentation stage: received water supply of "+ ((Water) msg).getAmount()+" litres");
        }
        else if(msg instanceof Sugar)
        {
            sugarAmount+= ((Sugar) msg).getAmount();
            System.out.println("Fermentation stage: received sugar supply of "+ ((Sugar) msg).getAmount()+" kg");
        }
        make();
        return this;
    }

    private boolean enoughSupplies()
    {
        if(sugarAmount<SUGAR_AMOUNT_NEEDED) return false;
        if(juiceAmount<JUICE_AMOUNT_NEEDED) return false;
        if(waterAmount<WATER_AMOUNT_NEEDED) return false;
        return true;
    }

    private boolean enoughSlots()
    {
        if(usedUpSlots>=SLOTS)
        {
            return false;
        }
        else return true;
    }

    private void make() throws InterruptedException {
        while(enoughSupplies())
        {
            if(enoughSlots())
            {
                System.out.println("Fermentation started");
                System.out.println("Using "+JUICE_AMOUNT_NEEDED+" amount of juice and "+
                        WATER_AMOUNT_NEEDED+" amount of water and "+
                        SUGAR_AMOUNT_NEEDED+" amount of sugar in slot number "+
                        usedUpSlots
                );
                children.get(usedUpSlots).tell(new StartChildMsg());
                usedUpSlots++;
                juiceAmount-=JUICE_AMOUNT_NEEDED;
                waterAmount-=WATER_AMOUNT_NEEDED;
                sugarAmount-=SUGAR_AMOUNT_NEEDED;
            }
            else {
                return;
            }
        }
        if(pressingFinished && usedUpSlots==0)
        {
            System.out.println("Fermentation stage finished production");
            storage.tell(new FermentationFinished());
            filtering.tell(new FermentationFinished());
        }
    }

    private Behavior<Msg> setPressingFinished(Msg msg) throws InterruptedException {
        pressingFinished=true;
        make();
        return this;
    }
}
