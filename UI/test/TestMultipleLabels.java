package alice.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;

import learn.ActiveLearning;
import learn.Learner;
import learn.Solution;
import util.ClauseUtils;
import alice.menu.BaseAction;
import alice.menu.DisableFeature;
import alice.menu.GenerateFacts;
import def.JavaSourceCodeInfo;
import def.PredicateDef;

public class TestMultipleLabels{

	List<String> currentGoldenTruth = new ArrayList<String>();
	List<String> currentFeatures = new ArrayList<String>();
	public static HashMap<String, HashMap<Integer, Integer>> fileToPosNegExamplesMap = new HashMap<String, HashMap<Integer,Integer>>();
	public static HashMap<String, HashMap<Integer, F1Score>> fileToF1ScoreMapGreedy = new HashMap<String, HashMap<Integer,F1Score>>();
	public static HashMap<String, HashMap<Integer, F1Score>> fileToF1ScoreMapExhaustive = new HashMap<String, HashMap<Integer,F1Score>>();

	public static HashMap<String, HashMap<Integer, HashMap<Integer, Double>>> varyFeaturePrecision = new HashMap<String, HashMap<Integer,HashMap<Integer,Double>>>();
	public static List<String> selectedExample = new ArrayList<String>();
	List<JavaSourceCodeInfo> posExamples = new ArrayList<JavaSourceCodeInfo>();
	List<JavaSourceCodeInfo> negativeExamples = new ArrayList<JavaSourceCodeInfo>();
	
	public static HashMap<String, HashMap<Integer, Integer>> fileToVaryExampleInIterationTwoMapTopDownExhaustive = new HashMap<String, HashMap<Integer,Integer>>();
	public static HashMap<String, HashMap<Integer, Integer>> fileToVaryExampleInIterationTwoMapTopDownGreedy = new HashMap<String, HashMap<Integer,Integer>>();

	public static HashMap<String, HashMap<Integer, F1Score>> fileToFeatureVaryF1MapExhaustive = new HashMap<String, HashMap<Integer,F1Score>>();
	public static HashMap<String, HashMap<Integer, F1Score>> fileToFeatureVaryF1MapGreedy = new HashMap<String, HashMap<Integer,F1Score>>();
	
	public static HashMap<Integer, Double> iterPrecisionSum = new HashMap<Integer, Double>();
	public static HashMap<Integer, Double> iterRecallSum = new HashMap<Integer, Double>();
	
	public static HashMap<Integer, HashMap<Integer, List<Integer>>> posNegThruAllIterG = new HashMap<Integer, HashMap<Integer,List<Integer>>>();	
	public static HashMap<Integer, HashMap<Integer, List<Integer>>> posNegThruAllIterE = new HashMap<Integer, HashMap<Integer,List<Integer>>>();	

	public List<PosNegMap> labelledExamplesData = new ArrayList<PosNegMap>();
	HashMap<Integer, List<JavaSourceCodeInfo>> accumulativeExamples = new HashMap<Integer, List<JavaSourceCodeInfo>>();
	
	HashMap<Integer, HashMap<Integer, List<JavaSourceCodeInfo>>> accumulativeIterExamples = new HashMap<Integer, HashMap<Integer,List<JavaSourceCodeInfo>>>();
	
	public HashMap<Integer, List<String>> iterEnabledStrings = new HashMap<Integer, List<String>>();
	
	public String folderName;
	public String biasType = "TopDown";
	public String baseFolder = "/home/whirlwind/Documents/ALICE_MULT_FEATURE/";
	
	HashMap<Integer, HashMap<Integer, HashMap<Integer, List<JavaSourceCodeInfo>>>> examplesAtEachIterAndEachTrial = new HashMap<Integer, HashMap<Integer,HashMap<Integer,List<JavaSourceCodeInfo>>>>();
	
	public void run(String fileName, List<String> candidates){
		String[] tmp = fileName.split("/");
		folderName = tmp[tmp.length-1].split("\\.")[0];
		System.out.println(folderName);
		
		getGroundTruth(fileName);
		getFeatures(fileName);
		getIterEnabledString(fileName);

		iterEnabledStrings.clear();
		getPrecisionForFeatures(candidates, fileName);
//		getPrecisions(candidates, fileName);
	}
	
	public void getIterEnabledString(String fileName){
		String file = fileName.split("[.]txt")[0]+"_Features"+".txt";
		readEnabledFile(file);
	}
	
	private void readEnabledFile(String fileName) {
		iterEnabledStrings.clear();
		File file = new File(fileName);
		try{
			String st;
			BufferedReader br = new BufferedReader(new FileReader(file));
			List<String> features = new ArrayList<String>();
			while ((st = br.readLine()) != null){
				features.add(st.toLowerCase());
			}
			int iterCount = 0;
			for(int i=0;i<features.size();){
				List<String> currentIterFeatures = new ArrayList<String>();
				currentIterFeatures.add(features.get(i));
				i++;
				currentIterFeatures.add(features.get(i));
				i++;
				iterEnabledStrings.put(iterCount, currentIterFeatures);
				iterCount++;
			}
		}catch(Exception e){
		}
	}

	public void getGroundTruth(String fileName){
		String file = fileName.split("[.]txt")[0]+"_Ground"+".txt";
		readFile(file);
	}
	
	public void getFeatures(String fileName){
		String file = fileName.split("[.]txt")[0]+"_Features"+".txt";
		readFeatureFile(file);
	}
	
	public void getPrecisionForFeatures(List<String> features,String fileName){				
		for(int i=1;i<5;i++){
			System.out.println("Running for varying feature "+i);
			getPrecisionEachFeature(i, features, 10,fileName);
			printPrecisionToFile(i);
			printEnabledStrings(fileName,i);					
		}
	}
	
	public void getPrecisions(List<String> features,String fileName){
		for(int i=0;i<10;i++){
			accumulativeIterExamples.put(i, new HashMap<Integer, List<JavaSourceCodeInfo>>());
			examplesAtEachIterAndEachTrial.put(i, new HashMap<Integer, HashMap<Integer, List<JavaSourceCodeInfo>>>());			
		}
		
		for(int i=2;i<6;i++){
			System.out.println("Running for varying feature "+i);
			getPrecisionEachIteration(2, i, features, 10,fileName);
			printPrecisionToFile(i);
			printPosNegToFileG(fileName);
			printPosNegToFileH(fileName);
			printEnabledStrings(fileName,i);					
		}
//		getPrecisionEachIteration(2, 3, features, 5,fileName);
//		printPrecisionToFile(3);
//		printPosNegToFileG(fileName);
//		printPosNegToFileH(fileName);
//		printEnabledStrings(fileName);
	}
	
	public void getPrecisionEachFeature(int noOfFeatures,List<String> features, int noOfIterations, String fileName){
		HashMap<Integer, List<HashMap<Integer,F1Score>>> convergeIterationF1Map = new HashMap<Integer, List<HashMap<Integer,F1Score>>>();
		
		StringBuilder builder = new StringBuilder();
		int original = noOfFeatures;
		try{
			  for(int i=0;i<noOfIterations;i++){
				System.out.println("At Iteration "+i);
				List<String> enabledString = new ArrayList<String>();
				List<Integer> indexArray = new ArrayList<Integer>();
				
                if(iterEnabledStrings.containsKey(i)){
                    enabledString.addAll(iterEnabledStrings.get(i));
                    noOfFeatures = original - enabledString.size();
                }

				for(int j=0;j<noOfFeatures;){
					int index = (int) (Math.random()*(features.size()));
					if(!enabledString.contains(features.get(index))){
						enabledString.add(features.get(index));	
						j++;
					}
				}			

                iterEnabledStrings.put(i,enabledString);
				
				builder.append(enabledString);
				builder.append("\n");
				
				System.out.println(enabledString);
				
				Learner learn = new Learner();
		        try{		        
		        	HashMap<Integer, F1Score> localPrecision = new HashMap<Integer, F1Score>();
		        	
		        	int numberOfActiveIteration = 1;
		        	
		        	learn.init(Extraction.extractFromBlockRelational.visitor.predicateDefiDefs, GenerateFacts.TypeFrequency,GenerateFacts.MethodFrequency, enabledString, DisableFeature.disabledSelectionStrings, Extraction.extractFromBlockRelational.visitor.generalisedPredicates);
		        	
		        	HashMap<Integer, Solution > solutionFromLearning = 
		        			new HashMap<Integer, Solution>(learn.solutionObjects);		        	

		        	builder.append("Trial "+i);
		        	builder.append("\n");
		        	
		        	builder.append("Iteration: "+numberOfActiveIteration);
		        	builder.append("\n");
		        	if(solutionFromLearning==null || solutionFromLearning.get(0)== null){
		        		int lastIndex = iterEnabledStrings.get(i).size()-1;
		        		iterEnabledStrings.get(i).remove(lastIndex);
		        		i--;
		        		continue;
		        	}
		        	builder.append(solutionFromLearning.get(0).queryAsString);
		        	builder.append("\n");
		        	
		        	F1Score score = new F1Score();
		        	score = calculatePrecisionAndRecall(solutionFromLearning);  		        	
		        	
        			localPrecision.put(numberOfActiveIteration, score);
		        	
		        	int numberofIter = localPrecision.size();
		        	if(!convergeIterationF1Map.containsKey(numberofIter)){
		        		convergeIterationF1Map.put(numberofIter, new ArrayList<HashMap<Integer,F1Score>>());
		        	} 		        	
		        	convergeIterationF1Map.get(numberofIter).add(localPrecision);		        
		        }catch(Exception e){	
		        	e.printStackTrace();
		        	System.out.println("exception in learning");
		        	i--;
		        	continue;
		        }		        		      
		    }
			  
			writeToFile(builder.toString(), fileName,"TopDownParent_"+original); 
			writeExamplesToFile(examplesAtEachIterAndEachTrial,fileName,"TopDown_"+original);			
			writeIterationPrecisionsToFile(convergeIterationF1Map, fileName,"TopDownParent_"+original);		 
		} catch(Exception e){
			System.out.println("Exception");
		}
	}
	private void createFolderIfNotExists(String folder){
		File file = new File(folder);
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("Directory is created!");
            } else {
                System.out.println("Failed to create directory!");
            }
        }               
	}
	private void printEnabledStrings(String fileName, int label){
		try {
//			fileName = fileName.split("\\.")[0];
			createFolderIfNotExists(baseFolder+biasType+"_"+label);
			createFolderIfNotExists(baseFolder+biasType+"_"+label+"/"+folderName);
			BufferedWriter writer = new BufferedWriter(new FileWriter
					(baseFolder+biasType+"/"+folderName+"/EnabledStrings_"+System.currentTimeMillis()+".txt"));
			StringBuilder builder = new StringBuilder();
			for(Integer key: iterEnabledStrings.keySet()){
				for(String feature: iterEnabledStrings.get(key)){
					builder.append(feature);
					builder.append("\n");
				}				
			}
			writer.write(builder.toString());
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void printPosNegToFileG(String fileName) {
		try {
			fileName = fileName.split("\\.")[0];
			BufferedWriter writer = new BufferedWriter(new FileWriter
					(baseFolder+biasType+"/"+folderName+"/E__G_"+System.currentTimeMillis()+".txt"));
			StringBuilder builder = new StringBuilder();
			for(Integer key: posNegThruAllIterG.keySet()){
				builder.append("-----Iter"+key+"-------");
				builder.append("\n");
				for(Integer iter: posNegThruAllIterG.get(key).keySet()){
					builder.append("\t\t"+iter+": pos - "+posNegThruAllIterG.get(key).get(iter).get(0));
					builder.append("\n");
					builder.append("\t\t\t"+": neg - "+posNegThruAllIterG.get(key).get(iter).get(1));
					builder.append("\n");
				}
			}			
			writer.write(builder.toString());
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void printPosNegToFileH(String fileName) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter
					(baseFolder+biasType+"/"+folderName+"/E__E_"+System.currentTimeMillis()+".txt"));
			StringBuilder builder = new StringBuilder();
			for(Integer key: posNegThruAllIterE.keySet()){
				builder.append("-----Iter"+key+"-------");
				builder.append("\n");
				for(Integer iter: posNegThruAllIterE.get(key).keySet()){
					builder.append("\t\t"+iter+": pos - "+posNegThruAllIterE.get(key).get(iter).get(0));
					builder.append("\n");
					builder.append("\t\t\t"+": neg - "+posNegThruAllIterE.get(key).get(iter).get(1));
					builder.append("\n");
				}
			}			
			writer.write(builder.toString());
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	private void printPrecisionToFile(int iter) {
		// TODO Auto-generated method stub
		try {
			iterPrecisionSum.clear();
			iterRecallSum.clear();
			createFolderIfNotExists(baseFolder+biasType);
			createFolderIfNotExists(baseFolder+biasType+"/"+folderName);
			BufferedWriter writer = new BufferedWriter(new FileWriter
					(baseFolder+biasType+"/"+folderName+"/E_"+iter+"_"+System.currentTimeMillis()+".txt"));
			StringBuilder builder = new StringBuilder();
			builder.append("----------- Greedy Score----------------");
			builder.append("\n");
			System.out.println("----------- Greedy Score----------------");
			for(String fileName: fileToF1ScoreMapGreedy.keySet()){
				System.out.println("Eval for "+fileName);
				builder.append("Eval for "+fileName);
				builder.append("\n");
				HashMap<Integer,F1Score> precisionMap = fileToF1ScoreMapGreedy.get(fileName);
				for(Integer key: precisionMap.keySet()){
					builder.append("#Iteration: "+key+" --- #Precision "+precisionMap.get(key).precision);				
					builder.append("\n");
					builder.append("\t\t "+key+" --- #Recall "+precisionMap.get(key).recall);
					builder.append("\n");
					builder.append("\t\t "+key+" --- #F1Score "+precisionMap.get(key).score);
					builder.append("\n");
					if(iterPrecisionSum.containsKey(key)){
						double sum = iterPrecisionSum.get(key);
						sum = sum + precisionMap.get(key).precision;
						iterPrecisionSum.put(key, sum);
						sum = iterRecallSum.get(key);
						sum = sum + precisionMap.get(key).recall;
						iterRecallSum.put(key, sum);
					} else{
						iterPrecisionSum.put(key, precisionMap.get(key).precision);
						iterRecallSum.put(key, precisionMap.get(key).recall);
					}
					System.out.println("#Iteration: "+key+" --- #Precision "+precisionMap.get(key).precision);
					System.out.println("\t\t "+key+" --- #Recall "+precisionMap.get(key).recall);
					System.out.println("\t\t "+key+" --- #F1Score "+precisionMap.get(key).score);
				}
			}
			
			builder.append("---SUM FOR ITER GREEDY-----");
			builder.append("\n");
			for(Integer key : iterPrecisionSum.keySet()){
				builder.append("Sum Precision Iter "+key+" "+iterPrecisionSum.get(key));
				builder.append("\n");
				builder.append("Sum Recall Iter "+key+" "+iterRecallSum.get(key));
				builder.append("\n");
			}
			
			iterPrecisionSum.clear();
			iterRecallSum.clear();
			
			System.out.println("----------- Exhaustive Score----------------");
			builder.append("----------- Exhaustive Score----------------");
			builder.append("\n");
			
			for(String fileName: fileToF1ScoreMapExhaustive.keySet()){
				System.out.println("Eval for "+fileName);
				builder.append("Eval for "+fileName);
				builder.append("\n");
				
				HashMap<Integer,F1Score> precisionMap = fileToF1ScoreMapExhaustive.get(fileName);
				for(Integer key: precisionMap.keySet()){
					builder.append("#Iteration: "+key+" --- #Precision "+precisionMap.get(key).precision);
					builder.append("\n");
					builder.append("\t\t "+key+" --- #Recall "+precisionMap.get(key).recall);
					builder.append("\n");
					builder.append("\t\t "+key+" --- #F1Score "+precisionMap.get(key).score);
					builder.append("\n");	
					if(iterPrecisionSum.containsKey(key)){
						double sum = iterPrecisionSum.get(key);
						sum = sum + precisionMap.get(key).precision;
						iterPrecisionSum.put(key, sum);
						sum = iterRecallSum.get(key);
						sum = sum + precisionMap.get(key).recall;
						iterRecallSum.put(key, sum);
					} else{
						iterPrecisionSum.put(key, precisionMap.get(key).precision);
						iterRecallSum.put(key, precisionMap.get(key).recall);
					}
					System.out.println("#Iteration: "+key+" --- #Precision "+precisionMap.get(key).precision);					
					System.out.println("\t\t "+key+" --- #Recall "+precisionMap.get(key).recall);
					System.out.println("\t\t "+key+" --- #F1Score "+precisionMap.get(key).score);
				}
			}
			
			builder.append("---SUM FOR ITER Exhaustive-----");
			builder.append("\n");
			for(Integer key : iterPrecisionSum.keySet()){
				builder.append("Sum Precision Iter "+key+" "+iterPrecisionSum.get(key));
				builder.append("\n");
				builder.append("Sum Recall Iter "+key+" "+iterRecallSum.get(key));
				builder.append("\n");
			}
			
			writer.write(builder.toString());
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	public List<Integer> returnNFeatureIndices(int size, int N){
		List<Integer> indices = new ArrayList<Integer>();
		for(int i=0;i<N;){
			int index = (int) (Math.random()*(size));
			if(!indices.contains(index)){
				indices.add(index);
				i++;
			}
		}
		return indices;
	}
	
	public boolean checkIfAlreadyExisits(HashMap<Integer,List<JavaSourceCodeInfo>> info, String value){
		boolean exists  = false;
		for(Integer key: info.keySet()){
			for(int i=0;i<info.get(key).size();i++){
				if(info.get(key).get(i).className.equals(value)){
					return true;
				}
			}
		}
		return exists;
	}
	
	public HashMap<Integer, List<JavaSourceCodeInfo>> returnNExamples(int N, HashMap<Integer,Solution> solutions, int trial, int exampleNeeded){
		HashMap<Integer, List<JavaSourceCodeInfo>> labelledExamples = new HashMap<Integer, List<JavaSourceCodeInfo>>();
		List<Integer> chosenIndex = new ArrayList<Integer>();		
		if(solutions.size() < N){
			N = solutions.size();
		}
		
		for(int i=0;i<N;){
			int index = (int) (Math.random()*(solutions.size()));
				if(!chosenIndex.contains(index)){
					if(exampleNeeded != 2 && !checkIfAlreadyExisits(accumulativeIterExamples.get(i), solutions.get(index).mapVartoSolution.get("X"))){
						chosenIndex.add(index);
						JavaSourceCodeInfo info = new JavaSourceCodeInfo();
						info.className = solutions.get(index).mapVartoSolution.get("X");					
						if(isPositive(solutions.get(index))){
							posExamples.add(info);
						}else{
							negativeExamples.add(info);
						}
						i++;
					} else{
						chosenIndex.add(index);
						JavaSourceCodeInfo info = new JavaSourceCodeInfo();
						info.className = solutions.get(index).mapVartoSolution.get("X");					
						if(isPositive(solutions.get(index))){
							posExamples.add(info);
						}else{
							negativeExamples.add(info);
						}
						i++;
					}
				}
		}		
		
		labelledExamples.put(1, posExamples);
		labelledExamples.put(2, negativeExamples);
		return labelledExamples;
	}
	
	public boolean isPositive(Solution sol){
		for(String pos: currentGoldenTruth){
			if(sol.mapVartoSolution.get("X").equals(pos)){
				return true;
			}
		}
		return false;
	}
	
	public List<JavaSourceCodeInfo> getPositives(int noOfPos){
		List<JavaSourceCodeInfo> posExamples = new ArrayList<JavaSourceCodeInfo>();
		for(int i=0;i<noOfPos;i++){
			JavaSourceCodeInfo info = new JavaSourceCodeInfo();
			info.className = currentGoldenTruth.get(i);
			posExamples.add(info);
		}
		return posExamples;
	}
	
	public int returnPositive(HashMap<Integer,Solution> solutions){		
		int noOfPos = 0;
		for(Integer key : solutions.keySet()){
			if(currentGoldenTruth.contains(solutions.get(key).mapVartoSolution.get("X"))){
				noOfPos++;				
			}
		}
		return noOfPos;
	}
	
	public List<JavaSourceCodeInfo> getNegatives(int noOfNeg, HashMap<Integer,Solution> solutions){
		List<JavaSourceCodeInfo> negExamples = new ArrayList<JavaSourceCodeInfo>();
		for(int i=0;i<noOfNeg;){
			Integer index = (int) (Math.random()*(solutions.size()));
			if(!currentGoldenTruth.contains(solutions.get(index).mapVartoSolution.get("X"))){
				JavaSourceCodeInfo info = new JavaSourceCodeInfo();
				info.className = solutions.get(index).mapVartoSolution.get("X");
				negExamples.add(info);
				i++;
			}
		}
		return negExamples;
	}
	
	public void readFile(String fileName){
		currentGoldenTruth.clear();
		File file = new File(fileName);
		try{
			String st;
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((st = br.readLine()) != null){
				currentGoldenTruth.add(st);
			}
		}catch(Exception e){
			
		}
	}
	
	public void readFeatureFile(String fileName){
		currentFeatures.clear();
		File file = new File(fileName);
		try{
			String st;
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((st = br.readLine()) != null){
				currentFeatures.add(st.toLowerCase());
			}
		}catch(Exception e){
			
		}
	}
	
	public void averageSolutions(int noOfFeatures, int noOfIterations, List<String> features){
		double precision = 0.0;
		try{
			  for(int i=0;i<noOfIterations;i++){
				List<String> enabledString = new ArrayList<String>();
				List<Integer> indexArray = new ArrayList<Integer>();
		
				for(int j=0;j<noOfFeatures;){
					int index = (int) (Math.random()*(features.size()));
					if(!indexArray.contains(index)){
						indexArray.add(index);				
						enabledString.add(features.get(index).replaceAll("\\\\", ""));	
						j++;
					}
				}				
				Learner learn = new Learner();
		        try{
		        	learn.init(Extraction.extractFromBlockRelational.visitor.predicateDefiDefs, GenerateFacts.TypeFrequency,GenerateFacts.MethodFrequency, enabledString, DisableFeature.disabledSelectionStrings, Extraction.extractFromBlockRelational.visitor.generalisedPredicates);
		        	//need method to calculate Precision at each iteration
//	        		precision = precision + calculatePrecision(learn.solutionObjects);
		        }catch(Exception e){			      
		        	System.out.println("Exception in learning.");
		        	i--;
		        	continue;
		        }			       			    					
		    }
		} catch(Exception e){
			System.out.println("Exception ");
		}
	}
	
	public F1Score calculatePrecisionAndRecall(HashMap<Integer, Solution> solutions){
		int relevantCount = 0;	
		for(Integer key: solutions.keySet()){
			if(currentGoldenTruth.contains(solutions.get(key).mapVartoSolution.get("X"))){
				relevantCount++;
			}
		}	
		F1Score score = new F1Score();
		score.precision = (relevantCount*1.0)/(solutions.size()*1.0);
		score.recall = (relevantCount*1.0)/(currentGoldenTruth.size()*1.0);
		
		return score;
	}
	
	public HashMap<Integer, List<JavaSourceCodeInfo>> createCopyOfMap(HashMap<Integer, List<JavaSourceCodeInfo>> examples){
		HashMap<Integer, List<JavaSourceCodeInfo>> copyMap = new HashMap<Integer, List<JavaSourceCodeInfo>>();
		for(Integer key : examples.keySet()){
			List<JavaSourceCodeInfo> listInfo = new ArrayList<JavaSourceCodeInfo>();
			for(int i=0;i<examples.get(key).size();i++){
				JavaSourceCodeInfo info = new JavaSourceCodeInfo();
				info.className = examples.get(key).get(i).className;
				listInfo.add(info);
			}
			copyMap.put(key, listInfo);
		}
		return copyMap;
	}
	
	public int returnSizeOfLabels (HashMap<Integer,List<JavaSourceCodeInfo>> labels){
		int count = 0;
		if(labels!=null){
			for(Integer key: labels.keySet()){
				count = count + labels.get(key).size();
			}
		}
		return count;
	}
	public void getPrecisionEachIteration(int noOfFeatures, int totalExamplesMarked, List<String> features, int noOfIterations, String fileName){
		HashMap<Integer, List<HashMap<Integer,F1Score>>> convergeIterationF1Map = new HashMap<Integer, List<HashMap<Integer,F1Score>>>();
		
		HashMap<Integer, List<HashMap<Integer,F1Score>>> convergeIterationF1MapRandom = new HashMap<Integer, List<HashMap<Integer,F1Score>>>();
	
		HashMap<Integer, List<HashMap<Integer,F1Score>>> convergeIterationF1MapAfter = new HashMap<Integer, List<HashMap<Integer,F1Score>>>();
		
		posNegThruAllIterE.clear();
		posNegThruAllIterG.clear();
		fileToF1ScoreMapExhaustive.clear();
		fileToF1ScoreMapGreedy.clear();
		
		StringBuilder builder = new StringBuilder();
		StringBuilder builderRandom = new StringBuilder();
		try{
			
			  for(int i=0;i<noOfIterations;i++){
			  		
				System.out.println("At Iteration "+i);
				List<String> enabledString = new ArrayList<String>();
				List<Integer> indexArray = new ArrayList<Integer>();
				
				for(int j=0;j<noOfFeatures;){
					int index = (int) (Math.random()*(features.size()));
					if(!indexArray.contains(index)){
						indexArray.add(index);				
						enabledString.add(features.get(index));	
						j++;
					}
				}			
								
//				enabledString.addAll(currentFeatures);
				if(totalExamplesMarked==2){
					List<String> copy = new ArrayList<String>();
					copy.addAll(enabledString);
					iterEnabledStrings.put(i,copy);
				} else{
					enabledString.clear();
					enabledString.addAll(iterEnabledStrings.get(i));
				}
				
//				if(!excep){
//					enabledString.clear();
//					enabledString.addAll(iterEnabledStrings.get(i));
//				} else{
//					iterEnabledStrings.put(i, enabledString);
//				}
				
//				enabledString.clear();
//				enabledString.addAll(iterEnabledStrings.get(i));
				
//				enabledString.clear();
//				enabledString.add("items==null");
//				enabledString.add("items[i]==null");
				
				builder.append(enabledString);
				builder.append("\n");
				
				System.out.println(enabledString);
				
				Learner learn = new Learner();
		        try{
		        	accumulativeExamples.clear();
		        	posExamples.clear();
		        	negativeExamples.clear();
		        	
		        	
		        	HashMap<Integer, F1Score> localPrecision = new HashMap<Integer, F1Score>();
		        	HashMap<Integer, F1Score> localPrecisionRandom = new HashMap<Integer, F1Score>();
		        	HashMap<Integer, F1Score> localPrecisionAfter = new HashMap<Integer, F1Score>();
		        	
		        	int numberOfActiveIteration = 1;
		        	
		        	learn.init(Extraction.extractFromBlockRelational.visitor.predicateDefiDefs, GenerateFacts.TypeFrequency,GenerateFacts.MethodFrequency, enabledString, DisableFeature.disabledSelectionStrings, Extraction.extractFromBlockRelational.visitor.generalisedPredicates);
		        	HashMap<Integer, Solution > firstSol = new HashMap<Integer, Solution>(learn.solutionObjects);		        	
		        	
		        	HashMap<Integer, Solution > firstSolCopy = new HashMap<Integer, Solution>(learn.solutionObjects);		        	
		        	
		        	HashMap<Integer, Solution > solutionFromLearning = 
		        			new HashMap<Integer, Solution>(learn.solutionObjects);		        	

		        	builder.append("Trial "+i);
		        	builder.append("\n");
		        	
		        	builder.append("Iteration: "+numberOfActiveIteration);
		        	builder.append("\n");
		        	builder.append(solutionFromLearning.get(0).queryAsString);
		        	builder.append("\n");
		        	
		        	int requiredExamples = totalExamplesMarked;	        		
		        	F1Score score = new F1Score();
		        	score = calculatePrecisionAndRecall(solutionFromLearning);  
		        	if(((int)score.precision == 1)){
		        		i--;
		        		System.out.println("Breaking because already it is precision 1");		        		
		        		continue;
		        	} else{
		        		if(((int)score.recall != 1)){
		        			i--;
		        			System.out.println("Breaking because already recall is not 1");
			        		continue;
		        		} else{
			        		if(learn.solutionObjects.size()<9){
			        			i--;
			        			continue;
			        		}
		        		}
		        	} 
		        	
		        	
        			localPrecision.put(numberOfActiveIteration, score);
        			
        			requiredExamples = totalExamplesMarked;
        			System.out.println("The size of returned examples is "+solutionFromLearning.size());        			
        			if(accumulativeExamples.containsKey(i)){
        				
        			}
        			
        			requiredExamples = totalExamplesMarked - returnSizeOfLabels(accumulativeIterExamples.get(i));
        			HashMap<Integer, List<JavaSourceCodeInfo>> returnedExamples = returnNExamples(requiredExamples, learn.solutionObjects,i,totalExamplesMarked);  
        			HashMap<Integer, List<JavaSourceCodeInfo>>  oldExamples = createCopyOfMap(accumulativeIterExamples.get(i));
        			if(oldExamples.containsKey(1)){
        				oldExamples.get(1).addAll(returnedExamples.get(1));
        				oldExamples.get(2).addAll(returnedExamples.get(2));
        			} else{
        				oldExamples = createCopyOfMap(returnedExamples);
        			}
        			
        			accumulativeIterExamples.put(i, oldExamples);
        			examplesAtEachIterAndEachTrial.get(i).put(numberOfActiveIteration, createCopyOfMap(oldExamples));
        			
        			HashMap<Integer, List<Integer>> posNegMap = new HashMap<Integer, List<Integer>>();
        			posNegMap.put(numberOfActiveIteration, new ArrayList<Integer>());
        			posNegMap.get(numberOfActiveIteration).add(returnedExamples.get(1).size());
        			posNegMap.get(numberOfActiveIteration).add(returnedExamples.get(2).size());
        			
        			ActiveLearning activeLearning = new ActiveLearning();
	        		activeLearning.labelledExamples = (HashMap<Integer, List<JavaSourceCodeInfo>>) returnedExamples;        				
	        		ActiveLearning.previousWasContainment = false;        			
        			        		        			
	        		System.out.println("Running for TopDown ");
	        		
	        		while(!((int)score.precision == 1)){	
//		        	while(numberOfActiveIteration<1){		        		
		        		numberOfActiveIteration++;
		        		
		        		solutionFromLearning = activeLearning.learnASeparatorTopDownExhaustive(
		        				solutionFromLearning.get(0).queryforSolution,
		        				solutionFromLearning.get(0).remainingPredicates,
		        				solutionFromLearning.get(0).originalSpecialisedPredicates);	
		        				        		
		        		score = new F1Score();
		        		score = calculatePrecisionAndRecall(solutionFromLearning);
		        		localPrecision.put(numberOfActiveIteration, score);
		        		activeLearning = new ActiveLearning();
		        		posExamples.clear();
		        		negativeExamples.clear();
		        		
		        		builder.append("Iteration: "+numberOfActiveIteration);
			        	builder.append("\n");
			        	builder.append(solutionFromLearning.get(0).queryAsString);
			        	builder.append("\n");
			        	
			        	if(solutionFromLearning.size() < requiredExamples){		        			
		        			activeLearning.labelledExamples = returnNExamples(solutionFromLearning.size(), solutionFromLearning,i, totalExamplesMarked);		       
		        		} else{
		        			activeLearning.labelledExamples = returnNExamples(totalExamplesMarked, solutionFromLearning,i, totalExamplesMarked);		       			        			
		        		}
			        	
			        	examplesAtEachIterAndEachTrial.get(i).put(numberOfActiveIteration, createCopyOfMap(activeLearning.labelledExamples));	        					        		
		        		
		        		posNegMap.put(numberOfActiveIteration, new ArrayList<Integer>());
	        			posNegMap.get(numberOfActiveIteration).add(activeLearning.labelledExamples.get(1).size());
	        			posNegMap.get(numberOfActiveIteration).add(activeLearning.labelledExamples.get(2).size());
		        	}	
		        	
//		        	System.out.println("Running for Random ");
//		        	score = localPrecisionRandom.get(numberOfActiveIterationRandom);
//		        	solutionFromLearning = firstSol;
//		        	activeLearning.labelledExamples = firstCopy;
//		        	
//		        	while(!((int)score.precision == 1)){		        		
//		        		numberOfActiveIterationRandom++;
//		        		
//		        		solutionFromLearning = activeLearning.learnASeparatorRandom(
//		        				solutionFromLearning.get(0).queryforSolution,
//		        				solutionFromLearning.get(0).remainingPredicates,
//		        				solutionFromLearning.get(0).originalSpecialisedPredicates,false);			        				        		
//		        		
//		        		score = new F1Score();
//		        		score = calculatePrecisionAndRecall(solutionFromLearning);
//		        		localPrecisionRandom.put(numberOfActiveIterationRandom, score);
//		        		activeLearning = new ActiveLearning();
//		        		posExamples.clear();
//		        		negativeExamples.clear();
//		        		
//		        		builderRandom.append("Iteration: "+numberOfActiveIteration);
//			        	builderRandom.append("\n");
//			        	builderRandom.append(solutionFromLearning.get(0).queryAsString);
//			        	builderRandom.append("\n");
//			        	
//		        		if(solutionFromLearning.size() < requiredExamples){		        			
//		        			activeLearning.labelledExamples = returnNExamples(solutionFromLearning.size(), solutionFromLearning);		       
//		        		} else{
//		        			activeLearning.labelledExamples = returnNExamples(totalExamplesMarked, solutionFromLearning);		       			        			
//		        		}
//		        		
//		        		posNegMap.put(numberOfActiveIteration, new ArrayList<Integer>());
//	        			posNegMap.get(numberOfActiveIteration).add(activeLearning.labelledExamples.get(1).size());
//	        			posNegMap.get(numberOfActiveIteration).add(activeLearning.labelledExamples.get(2).size());
//		        	}	
//		        	
//		        	posNegThruAllIterE.put(i, posNegMap);
//		        	System.out.println("Running for After ");
//		        	
//		        	score = localPrecisionAfter.get(numberOfActiveIterationAfter);
//		        	solutionFromLearning = firstSolCopy;
//		        	activeLearning.labelledExamples = afterCopy;
//		        	
//		        	while(!((int)score.precision == 1)){		        		
//		        		numberOfActiveIterationAfter++;
//		        				        					        		
//		        		solutionFromLearning = activeLearning.learnASeparatorTopologicalOrder(
//		        				solutionFromLearning.get(0).queryforSolution,
//		        				solutionFromLearning.get(0).remainingPredicates,
//		        				solutionFromLearning.get(0).originalSpecialisedPredicates,false);
////		        		
//		        		score = new F1Score();
//		        		score = calculatePrecisionAndRecall(solutionFromLearning);
//		        		localPrecisionAfter.put(numberOfActiveIterationAfter, score);
//		        		activeLearning = new ActiveLearning();
//		        		posExamples.clear();
//		        		negativeExamples.clear();
//		        		
//		        		builderAfter.append("Iteration: "+numberOfActiveIterationAfter);
//		        		builderAfter.append("\n");
//		        		builderAfter.append(solutionFromLearning.get(0).queryAsString);
//		        		builderAfter.append("\n");
//			        	
//		        		if(solutionFromLearning.size() < requiredExamples){		        			
//		        			activeLearning.labelledExamples = returnNExamples(solutionFromLearning.size(), solutionFromLearning);		       
//		        		} else{
//		        			activeLearning.labelledExamples = returnNExamples(totalExamplesMarked, solutionFromLearning);		       			        			
//		        		}
//		        		
//		        		posNegMap.put(numberOfActiveIterationAfter, new ArrayList<Integer>());
//	        			posNegMap.get(numberOfActiveIterationAfter).add(activeLearning.labelledExamples.get(1).size());
//	        			posNegMap.get(numberOfActiveIterationAfter).add(activeLearning.labelledExamples.get(2).size());
//		        	}	
		        	
		        	int numberofIter = localPrecision.size();
		        	if(!convergeIterationF1Map.containsKey(numberofIter)){
		        		convergeIterationF1Map.put(numberofIter, new ArrayList<HashMap<Integer,F1Score>>());
		        	} 		        	
		        	convergeIterationF1Map.get(numberofIter).add(localPrecision);
		        	numberofIter = localPrecisionRandom.size();
		        	if(!convergeIterationF1MapRandom.containsKey(numberofIter)){
		        		convergeIterationF1MapRandom.put(numberofIter, new ArrayList<HashMap<Integer,F1Score>>());
		        	} 		        	
		        	convergeIterationF1MapRandom.get(numberofIter).add(localPrecisionRandom);
		        	
		        	numberofIter = localPrecisionAfter.size();
		        	if(!convergeIterationF1MapAfter.containsKey(numberofIter)){
		        		convergeIterationF1MapAfter.put(numberofIter, new ArrayList<HashMap<Integer,F1Score>>());
		        	} 		        	
		        	convergeIterationF1MapAfter.get(numberofIter).add(localPrecisionAfter);
		        
		        }catch(Exception e){	
		        	e.printStackTrace();
		        	System.out.println("exception in learning");
		        	i--;
		        	continue;
		        }		        		      
		    }
			  
			writeToFile(builder.toString(), fileName,"TopDownParent_"+totalExamplesMarked); 
//			writeToFile(builderRandom.toString(), fileName,"RandomParent"); 
//			writeToFile(builderAfter.toString(), fileName,"AfterParent");
			writeExamplesToFile(examplesAtEachIterAndEachTrial,fileName,"TopDown_"+totalExamplesMarked);			
			writeIterationPrecisionsToFile(convergeIterationF1Map, fileName,"TopDownParent_"+totalExamplesMarked);		 
//			writeIterationPrecisionsToFile(convergeIterationF1MapRandom, fileName,"RandomParent");
//			writeIterationPrecisionsToFile(convergeIterationF1MapAfter, fileName,"AfterParent");
		} catch(Exception e){
			System.out.println("Exception");
		}
	}

	private void writeExamplesToFile(
			HashMap<Integer, HashMap<Integer, HashMap<Integer, List<JavaSourceCodeInfo>>>> map,
			String fileName, String folder) {
		try {
		for(Integer trial : map.keySet()){			
			createFolderIfNotExists(baseFolder+"ExampleLog");
			createFolderIfNotExists(baseFolder+"ExampleLog/"+folder);
//			createFolderIfNotExists(baseFolder+"ExampleLog/"+folder+"/Iter_"+key);
			BufferedWriter writer;
			
				writer = new BufferedWriter(
						new FileWriter
					(baseFolder+"ExampleLog/"+folder+"/ExampleLogData"+".txt", true));
				writer.write(fileName);
				writer.write("\n");
				writer.write("Start Trial : "+trial);
				writer.write("\n");
				for(Integer iter: map.get(trial).keySet()){
					writer.write("Start Iter : "+iter);
					writer.write("\n");
					for(Integer exampleType: map.get(trial).get(iter).keySet()){
						String parent = "";
						if(exampleType==1){
							parent = "pos";
						} else{
							parent = "neg";
						}
						StringBuilder builder = new StringBuilder();
						for(int i=0;i<map.get(trial).get(iter).get(exampleType).size();i++){
							builder.append(map.get(trial).get(iter).get(exampleType).get(i).className);
							builder.append(",");
						}
						
						writer.write(parent+":"+builder.toString());
						writer.write("\n");
					}
					
					writer.write("End Iter : "+iter);
					writer.write("\n");
				}
				writer.write("End Trial : "+trial);
				writer.write("\n");
				writer.close();
			} 			
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void writeIterationPrecisionsToFile(
			HashMap<Integer, List<HashMap<Integer, F1Score>>> convergeIterationF1Map, 
			String fileName, String folder) {
		
		try {
			for(Integer key: convergeIterationF1Map.keySet()){
				
				createFolderIfNotExists(baseFolder+"IterationPrecision");
				createFolderIfNotExists(baseFolder+"IterationPrecision/"+folder);
				createFolderIfNotExists(baseFolder+"IterationPrecision/"+folder+"/Iter_"+key);
				BufferedWriter writer = new BufferedWriter(
						new FileWriter
					(baseFolder+"IterationPrecision/"+folder+"/Iter_"+key+"/RunData"+".txt", true));
				writer.write(fileName);
				writer.write("\n");
				for(int i=0;i<convergeIterationF1Map.get(key).size();i++){
					StringBuilder builder = new StringBuilder();
					builder.append("Iteration_Start");
					builder.append("\n");
					for(Integer iterKey : convergeIterationF1Map.get(key).get(i).keySet()){
						String toPrint = convergeIterationF1Map.get(key).get(i).get(iterKey).precision
								+","+convergeIterationF1Map.get(key).get(i).get(iterKey).recall
								+","+convergeIterationF1Map.get(key).get(i).get(iterKey).score;
						builder.append(toPrint);
						builder.append("\n");						
					}
					builder.append("Iteration_End");
					builder.append("\n");
					writer.append(builder);
				}				
				writer.close();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

	private void writeToFile(String string, String fileName, String folder) {
		try {
			createFolderIfNotExists(baseFolder);
			createFolderIfNotExists(baseFolder+folder);
			createFolderIfNotExists(baseFolder+folder+"/"+folderName);
			BufferedWriter writer = new BufferedWriter(new FileWriter
					(baseFolder+folder+"/"+folderName+"/RunData"+System.currentTimeMillis()+".txt"));
			writer.write(fileName+"\n");
			writer.write(string);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
}
