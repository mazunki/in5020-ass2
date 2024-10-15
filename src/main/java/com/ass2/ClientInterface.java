package com.ass2;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface ClientInterface {
    public BigDecimal getQuickBalance();

	public BigDecimal getSyncedBalance();
	
	public List<Transaction> getHistory();

	public boolean checkTxStatus(String transactionId);

	public void cleanHistory();

	public Collection<String> memberInfo(); 

	public void sleep(int seconds);

	public void exit();   
}