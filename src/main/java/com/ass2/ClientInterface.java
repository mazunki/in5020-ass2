package com.ass2;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface ClientInterface {
    public BigDecimal getQuickBalance();

	public BigDecimal getSyncedBalance();

	public void deposit(BigDecimal amount);

	public void deposit(int amount);
	public void deposit(float amount);
	
	public int addInterest(int interest);

	public List<Transaction> getHistory();

	public boolean checkTxStatus(String transactionId);

	public void cleanHistory();

	public Collection<String> memberInfo();

	public void sleep(int seconds);

	public void exit();
    
}