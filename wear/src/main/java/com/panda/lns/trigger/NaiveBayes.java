package com.panda.lns.trigger;

public class NaiveBayes {
	Class[] classes=null;
	NaiveBayes(Class[] classes){
		this.classes=classes;
	}
	int getPrediction(double[] values){
		double[] probabilities = new double[classes.length];
		double tmp=1;
		for(int i=0;i<classes.length;i++){
			for(int k=0;k<classes[i].getPredictors().length;k++){
				tmp*=classes[i].getPredictors()[k].getProbability(values[k]);
			}
			tmp*=classes[i].getP();
			probabilities[i] = tmp;
			tmp=1;
		}
		double max=0;
		int maxIndex=-1;
		for(int i=0;i<probabilities.length;i++){
			if(probabilities[i] > max){
				max=probabilities[i];
				maxIndex=i;
			}
		}
		return maxIndex;
	}
}

class Class{
	private double p=0;
	private String name="";
	private Predictor[] predictors=null;
	Class(String name, double p, Predictor[] predictors){
		this.name=name;
		this.p=p;
		this.predictors=predictors;
	}
	double getP(){
		return p;
	}
	String getName(){
		return name;
	}
	Predictor[] getPredictors(){
		return predictors;
	}
}

class Predictor{
	private double sd=0;
	private double mean=0;
	Predictor(double mean, double sd){
		this.mean=mean;
		this.sd=sd;
	}
	double getProbability(double value){
		return ((Math.pow(2.71828, -Math.pow((value - mean), 2)/(2*sd*sd)))/sd);
	}
}
