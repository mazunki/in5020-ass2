package com.ass2;

import java.math.BigDecimal;


public class Account implements TransactionInterface {
    private BigDecimal balance;
    private final String accountName;

    public Account(String accountName) {
        this.accountName = accountName;
        this.balance = new BigDecimal(0);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    @Override
    public void deposit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    @Override
    public void deposit(int amount) {
        deposit(new BigDecimal(amount));
    }

    @Override
    public void deposit(float amount) {
        deposit(new BigDecimal(amount));
    }

    @Override
    public void addInterest(int interest) {
        BigDecimal interestAmount = balance.multiply(new BigDecimal(interest)).divide(new BigDecimal(100));
        deposit(interestAmount);
    }

    @Override
    public void withdraw(BigDecimal amount) {
        balance = balance.subtract(amount);
    }

    @Override
    public void withdraw(int amount) {
        withdraw(new BigDecimal(amount));
    }

    @Override
    public void withdraw(float amount) {
        withdraw(new BigDecimal(amount));
    }

    public String getAccountName() {
        return accountName;
    }
}
