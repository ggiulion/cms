/*
Copyright 2018 Gianfranco Giulioni

This file is part of the Commodity Market Simulator (CMS):

    CMS is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    CMS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CMS.  If not, see <http://www.gnu.org/licenses/>.
*/


package cms;

import cms.agents.Producer;
import cms.agents.Buyer;
import cms.agents.Market;
import cms.utils.MarketLongitudeComparator;
import cms.dynamics.Cms_scheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.math.BigDecimal;
import java.math.RoundingMode;

import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.gis.GeographyFactory;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.gis.display.RepastMapLayer;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.util.collections.IndexedIterable;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.parameter.Parameters;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import org.geotools.referencing.GeodeticCalculator;

public class Cms_builder implements ContextBuilder<Object> {
	public static boolean verboseFlag=false;
	public static boolean autarkyAtTheBeginning=true;
	Producer aProducer;
	Buyer aBuyer;
	Market aMarket;
	Coordinate coord;
	Point geom;
	RepastMapLayer mapLayer;
	List<String> lines;
	String tmpVarieties="";
	String tmpMarkets="";
	ArrayList<Market> marketsList;
	IndexedIterable<Object> producersList,buyersList;	
	public ArrayList<Double> bidAndAskPrices;
	public static GeodeticCalculator distanceCalculator;
	public static ISchedule schedule;
	private boolean tmpMustImportFlag;

	
	int batchStoppingTime=2;
	public static int productionCycleLength,exportPolicyDecisionInterval,importPolicyDecisionInterval,globalProduction,minimumImportQuantity,producersPricesMemoryLength;
	public static double consumptionShareToSetMinimumConsumption,consumptionShareToSetMaximumConsumption,productionRateOfChangeControl,probabilityToAllowExport,probabilityToAllowImport,toleranceInMovingDemand,shareOfDemandToBeMoved,percentageOfPriceMarkDownInNewlyAccessibleMarkets,weightOfDistanceInInitializingIntercept,percentageChangeInTargetProduction,priceThresholdToIncreaseTargetProduction,priceThresholdToDecreaseTargetProduction;


	@Override
public Context<Object> build(Context<Object> context) {


	Parameters params = RunEnvironment.getInstance().getParameters();
	verboseFlag=(boolean)params.getValue("verboseFlag");
	autarkyAtTheBeginning=(boolean)params.getValue("autarkyAtTheBeginning");
	productionCycleLength=(int)params.getValue("productionCycleLength");
	exportPolicyDecisionInterval=(int)params.getValue("exportPolicyDecisionInterval");
	importPolicyDecisionInterval=(int)params.getValue("importPolicyDecisionInterval");
	globalProduction=(int)params.getValue("globalProduction");
	producersPricesMemoryLength=(int)params.getValue("producersPricesMemoryLength");
	minimumImportQuantity=(int)params.getValue("minimumImportQuantity");
	weightOfDistanceInInitializingIntercept=(double)params.getValue("weightOfDistanceInInitializingIntercept");
	consumptionShareToSetMinimumConsumption=(double)params.getValue("consumptionShareToSetMinimumConsumption");
	consumptionShareToSetMaximumConsumption=(double)params.getValue("consumptionShareToSetMaximumConsumption");
	productionRateOfChangeControl=(double)params.getValue("productionRateOfChangeControl");
	probabilityToAllowExport=(double)params.getValue("probabilityToAllowExport");
	probabilityToAllowImport=(double)params.getValue("probabilityToAllowImport");
	toleranceInMovingDemand=(double)params.getValue("toleranceInMovingDemand");
	shareOfDemandToBeMoved=(double)params.getValue("shareOfDemandToBeMoved");
	percentageOfPriceMarkDownInNewlyAccessibleMarkets=(double)params.getValue("percentageOfPriceMarkDownInNewlyAccessibleMarkets");
	percentageChangeInTargetProduction=(double)params.getValue("percentageChangeInTargetProduction");
	priceThresholdToIncreaseTargetProduction=(double)params.getValue("priceThresholdToIncreaseTargetProduction");
	priceThresholdToDecreaseTargetProduction=(double)params.getValue("priceThresholdToDecreaseTargetProduction");
	batchStoppingTime=(int)params.getValue("batchStoppingTime");

	GeographyParameters<Object> geoParams = new GeographyParameters<Object>();
	GeographyFactory factory = GeographyFactoryFinder.createGeographyFactory(null);
	Geography<Object> geography = factory.createGeography("Geography", context, geoParams);
	GeometryFactory fac = new GeometryFactory();

	distanceCalculator=new GeodeticCalculator(geography.getCRS());

	System.out.println();
	if(verboseFlag){
		System.out.println();
		System.out.println("===================================================================");
		System.out.println("BEGIN INITIALIZATION");
		System.out.println("====================================================================");
		System.out.println("");
	}

	if(verboseFlag){
		System.out.println("");
	}
	bidAndAskPrices=new ArrayList<Double>();
	for(int i=0;i<1000;i++){
		bidAndAskPrices.add((new BigDecimal(i*0.01)).setScale(2,RoundingMode.HALF_EVEN).doubleValue());
	}


	//Producers creation
	try{
		lines=Files.readAllLines(Paths.get(System.getProperty("user.dir")+"/data/producers.csv"), Charset.forName("UTF-8"));
	} catch (IOException e) {
		e.printStackTrace();
	}
	for(int i=1;i<lines.size()-1;i++){
		String[] parts = ((String)lines.get(i)).split(",");
		aProducer=new Producer(parts[0],new Double(parts[1]),new Double(parts[2]),new Double(parts[3]),parts[4],parts[5],bidAndAskPrices);
		aProducer.setup((new Integer(parts[6])).intValue());
		tmpMarkets=tmpMarkets+"|"+parts[4];
		tmpVarieties=tmpVarieties+"|"+parts[5];
		context.add(aProducer);
		coord = new Coordinate(aProducer.getLongitude(),aProducer.getLatitude());
		geom = fac.createPoint(coord);
		geography.move(aProducer, geom);
	}
	if(verboseFlag){
		System.out.println("");
	}
	//Buyers creation
	try{
		lines=Files.readAllLines(Paths.get(System.getProperty("user.dir")+"/data/buyers.csv"), Charset.forName("UTF-8"));
	} catch (IOException e) {
		e.printStackTrace();
	}
	for(int i=1;i<lines.size()-1;i++){
		String[] parts = ((String)lines.get(i)).split(",");
		aBuyer=new Buyer(parts[0],new Double(parts[1]),new Double(parts[2]),new Double(parts[3]),bidAndAskPrices,new Integer(parts[4]),new Integer(parts[5]));
		context.add(aBuyer);
		coord = new Coordinate(aBuyer.getLongitude(),aBuyer.getLatitude());
		geom = fac.createPoint(coord);
		geography.move(aBuyer, geom);
	}
	if(verboseFlag){
		System.out.println("");
	}
	//Markets creation


	marketsList=new ArrayList<Market>();

	try{
		lines=Files.readAllLines(Paths.get(System.getProperty("user.dir")+"/data/markets.csv"), Charset.forName("UTF-8"));
	} catch (IOException e) {
		e.printStackTrace();
	}
	for(int i=1;i<lines.size()-1;i++){
		String[] parts = ((String)lines.get(i)).split(",");
		aMarket=new Market(parts[0],new Double(parts[1]),new Double(parts[2]),new Double(parts[3]));
		marketsList.add(aMarket);
	}

	Collections.sort(marketsList,new MarketLongitudeComparator());

	if(verboseFlag){
		System.out.println();
		System.out.println("markets sorted according to their longitude");
	}
	for(Market aMarket : marketsList){
		if(verboseFlag){
			System.out.println("       market "+aMarket.getName()+" longitude "+aMarket.getLongitude());
		}
		context.add(aMarket);
		coord = new Coordinate(aMarket.getLongitude(),aMarket.getLatitude());
		geom = fac.createPoint(coord);
		geography.move(aMarket, geom);
	}





	//check markets and varieties

	String[] partsTmpMarkets=tmpMarkets.split("\\|");
	ArrayList<String> markets= new ArrayList<String>();
	markets.add(partsTmpMarkets[1]);
	for(int i=2;i<partsTmpMarkets.length;i++){
		boolean isPresent=false;
		for(int j=0;j<markets.size();j++){
			if(partsTmpMarkets[i].equals((String)markets.get(j))){
				isPresent=true;
			}
		}
		if(!isPresent){
			markets.add(partsTmpMarkets[i]);
		}
	}
	if(verboseFlag){
		System.out.println("");
		System.out.println("The following markets were found in the producers configuration file:");
		for(int j=0;j<markets.size();j++){
			System.out.println("     "+markets.get(j));
		}
		System.out.println("Please cross check that all the markets included in the markets configuration file are listed above");
		System.out.println("");
	}

	String[] partsTmpVarieties=tmpVarieties.split("\\|");
	ArrayList<String> varieties= new ArrayList<String>();
	varieties.add(partsTmpVarieties[1]);
	for(int i=2;i<partsTmpVarieties.length;i++){
		boolean isPresent=false;
		for(int j=0;j<varieties.size();j++){
			if(partsTmpVarieties[i].equals((String)varieties.get(j))){
				isPresent=true;
			}
		}
		if(!isPresent){
			varieties.add(partsTmpVarieties[i]);
		}
	}

	if(verboseFlag){
		System.out.println("The following products were found in the producers configuration file:");
		for(int j=0;j<varieties.size();j++){
			System.out.println("     "+varieties.get(j));
		}
		System.out.println("Please cross check against typos");
		System.out.println("");
	}

	//Create Market sessions
	try{
		buyersList=context.getObjects(Class.forName("cms.agents.Buyer"));
		producersList=context.getObjects(Class.forName("cms.agents.Producer"));
	}
	catch(ClassNotFoundException e){
		System.out.println("Class not found");
	}
	for(int i=0;i<producersList.size();i++){
		aProducer=(Producer)producersList.get(i);
		String[] aProducerTmpMarkets=aProducer.getMarkets().split("\\|");
		for(int j=0;j<aProducerTmpMarkets.length;j++){
			for(int k=0;k<marketsList.size();k++){
				aMarket=(Market)marketsList.get(k);
				if(aProducerTmpMarkets[j].equals(aMarket.getName())){
					if(verboseFlag){
						System.out.println("MARKET "+aMarket.getName()+" ADD SESSIONS FOR "+aProducer.getName());
					}
					aMarket.addMarketSessions(aProducer,aProducer.getVarieties(),context,bidAndAskPrices);
				}
			}
		}
	}

	if(verboseFlag){
		System.out.println();
	}
	//setting buyers mustImportFlag
	for(int i=0;i<buyersList.size();i++){
		tmpMustImportFlag=true;
		aBuyer=(Buyer)buyersList.get(i);
		for(int j=0;j<producersList.size();j++){
			aProducer=(Producer)producersList.get(j);
			if(aProducer.getName().equals(aBuyer.getName())){
				tmpMustImportFlag=false;
			}
		}
		aBuyer.setMustImportFlag(tmpMustImportFlag);
	}


	//System.out.println("Scheduling events");
	if(verboseFlag){
		System.out.println("");
	}
	schedule = RunEnvironment.getInstance().getCurrentSchedule();
	Cms_scheduler cms_schduler=new Cms_scheduler(context);
	cms_schduler.scheduleEvents();

	if(verboseFlag){
		System.out.println();
		System.out.println("===================================================================");
		System.out.println("END INITIALIZATION");
		System.out.println("====================================================================");
		System.out.println();
	}


	if (RunEnvironment.getInstance().isBatch())
	{
		RunEnvironment.getInstance().endAt(batchStoppingTime);
	}


	return context;
	}



}
