package com.miccelinski.gps;

public class Spot {

	public String Name;
	public double[] coordinates = { 0, 0 };
	
	/**
	 * @param Name - Set name
	 * @param Y - latitude
	 * @param X - longtitude
	 */
	public Spot(String Name, double Y, double X){
		this.Name = Name;
		coordinates[0] = Y;
		coordinates[1] = X;
	}
}
