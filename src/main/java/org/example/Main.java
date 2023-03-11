package org.example;


import akka.actor.typed.ActorSystem;
import org.example.actors.StorageActor;
import org.example.msg.Msg;
import org.example.msg.StartMsg;

import java.util.Scanner;

import static java.lang.Thread.sleep;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        System.out.println("Start simulation");
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter initial amount of:");
        System.out.println("-grapes");
        int grapesAmount= scan.nextInt();
        System.out.println("-water");
        int waterAmount = scan.nextInt();
        System.out.println("-sugar");
        int sugarAmount = scan.nextInt();
        System.out.println("-bottles");
        int bottlesAmount = scan.nextInt();
        System.out.println("Enter the speed-up factor:");
        int speedUpFactor = scan.nextInt();

        final ActorSystem<Msg> actorSystem = ActorSystem.create(StorageActor.create(grapesAmount,waterAmount,sugarAmount,bottlesAmount,speedUpFactor),"name");
        actorSystem.tell(new StartMsg());
    }
}