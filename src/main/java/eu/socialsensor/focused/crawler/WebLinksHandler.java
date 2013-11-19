package eu.socialsensor.focused.crawler;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import eu.socialsensor.focused.crawler.bolts.ArticleExtractionBolt;
import eu.socialsensor.focused.crawler.bolts.MediaExtractionBolt;
import eu.socialsensor.focused.crawler.bolts.RankerBolt;
import eu.socialsensor.focused.crawler.bolts.URLExpanderBolt;
import eu.socialsensor.focused.crawler.bolts.UpdaterBolt;
import eu.socialsensor.focused.crawler.spouts.MongoDbInjector;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.topology.TopologyBuilder;


public class WebLinksHandler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		String mongoHost = args[0];
		String mongoDbName = args[1]; 
		String mongoCollection = args[2];
		String mediaCollection = args[3];
		
//		String mongoHost = "social1.atc.gr";
//		String mongoDbName = "Streams"; 
//		String mongoCollection = "WebPages";
//		String mediaCollection = "MediaItems";
		
			
		DBObject query = new BasicDBObject("status", "new");
	
		URLExpanderBolt urlExpander;
		try {
			urlExpander = new URLExpanderBolt(mongoHost, mongoDbName, mongoCollection);
		} catch (Exception e) {
			return;
		}
		
		// Create topology 
		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("injector", new MongoDbInjector(mongoHost, mongoDbName, mongoCollection, query), 1);
        
		builder.setBolt("ranker", new RankerBolt(), 1).shuffleGrouping("injector");
		builder.setBolt("expander", urlExpander, 8).shuffleGrouping("ranker");
		builder.setBolt("articleExtraction",  new ArticleExtractionBolt(60), 1).shuffleGrouping("expander", "article");
		builder.setBolt("mediaExtraction",  new MediaExtractionBolt(), 4).shuffleGrouping("expander", "media");
		
		builder.setBolt("updater",  new UpdaterBolt(mongoHost, mongoDbName, mongoCollection, mediaCollection), 4)
			.shuffleGrouping("articleExtraction").shuffleGrouping("mediaExtraction");
		
		//builder.setBolt("metrics", new MetricsBolt(), 1).shuffleGrouping("updater");
		
        Config conf = new Config();
        conf.setDebug(false);
       
		//while(true) {
			try {
        // Run topology
//        if(args!=null && (args.length == 1 || args.length == 2)) {
//        	int workers = 2;
//        	if(args.length>1) {
//        		try {
//        		workers = Integer.parseInt(args[1]);
//        		}
//        		catch(NumberFormatException e) {
//        			System.out.println(e.getMessage());
//        		}
//        	}
//        	
//            conf.setNumWorkers(workers);
//            try {
//				StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
//			} catch (Exception e) {
//				System.out.print(e.getMessage());
//			}
//        } else {
       System.out.println("Submit topology to local cluster");
       LocalCluster cluster = new LocalCluster();
       cluster.submitTopology("twitter", conf, builder.createTopology());
//        }
        
//		}
        //Utils.sleep(300000);
        //cluster.shutdown();
		}
			catch(Exception e) {
				e.printStackTrace();
			}
		//}
		
	}
}