package com.ass2;

import java.math.BigDecimal;

public interface TransactionInterface {
    public void deposit(BigDecimal amount);
    public void deposit(int amount);
    public void deposit(float amount);

    public void withdraw(BigDecimal amount);
    public void withdraw(int amount);
    public void withdraw(float amount);

    public void addInterest(int interest);
}

