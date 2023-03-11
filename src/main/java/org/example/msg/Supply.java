package org.example.msg;

public abstract class Supply implements Msg{

    private int amount;

    public Supply(int amount)
    {
        this.amount=amount;
    }

    public int getAmount() {
        return amount;
    }
}
