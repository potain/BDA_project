
/**
   @author Bo Wang

   This class is a stub for VFDT. 

   (c) 2016
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Vfdt extends IncrementalLearner<Integer> {

	private int[] nbFeatureValues;
	private double delta;
	private double tau;
	private int nmin;

	private VfdtNode root;

	/**
	 * Vfdt constructor
	 * 
	 * 
	 * @param nbFeatureValues
	 *            are nb of values of each feature. e.g. nbFeatureValues[3]=5
	 *            means that feature 3 can have values 0,1,2,3 and 4.
	 * @param delta
	 *            is the parameter used for the Hoeffding bound
	 * @param tau
	 *            is the parameter that is used to deal with ties
	 * @param nmin
	 *            is the parameter that is used to limit the G computations
	 */
	public Vfdt(int[] nbFeatureValues, double delta, double tau, int nmin) {
		this.nbFeatureValues = nbFeatureValues;
		this.delta = delta;
		this.tau = tau;
		this.nmin = nmin;

		nbExamplesProcessed = 0;
		int[] possibleFeatures = new int[nbFeatureValues.length];
		for (int i = 0; i < nbFeatureValues.length; i++)
			possibleFeatures[i] = i;
		this.root = new VfdtNode(nbFeatureValues, possibleFeatures);
	}

	/**
	 * This method will update the parameters of you model using the given
	 * example.
	 * 
	 * 
	 * @param example
	 *            is a training example
	 */
	@Override
	public void update(Example<Integer> example) {
		super.update(example);
		this.root.updateExample(example, this.nmin, this.delta, this.tau);
	}

	/**
	 * Uses the current model to calculate the probability that an
	 * attributeValues belongs to class "1";
	 * 
	 * THIS METHOD IS REQUIRED
	 * 
	 * @param example
	 *            is a the test instance to classify
	 * @return the probability that attributeValues belongs to class "1"
	 */
	@Override
	public double makePrediction(Integer[] example) {

		double prediction = 0;
		prediction = this.root.makePrediction(example);

		return prediction;
	}

	/**
	 * Writes the current model to a file.
	 *
	 * The written file can be read in with readModel.
	 *
	 *
	 * @param path
	 *            the path to the file
	 * @throws IOException
	 */
	@Override
	public void writeModel(String path) throws IOException {

		try {
			File file = new File(path);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));

			String content = this.root.generateModelString(this.nbFeatureValues);

			bw.write(content);
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads in the model in the file and sets it as the current model. Sets the
	 * number of examples processed.
	 *
	 *
	 * @param path
	 *            the path to the model file
	 * @param nbExamplesProcessed
	 *            the nb of examples that were processed to get to the model in
	 *            the file.
	 * @throws IOException
	 */
	@Override
	public void readModel(String path, int nbExamplesProcessed) throws IOException {
		super.readModel(path, nbExamplesProcessed);

		/* FILL IN HERE */
		BufferedReader reader = null;
		int contentListLength = 0;
		List<String> contentList = new ArrayList<String>();
		try {
			reader = new BufferedReader(new FileReader(path));
			contentListLength = Integer.parseInt(reader.readLine()); // readfirstLine
			String line;
			while ((line = reader.readLine()) != null) {
				// System.out.println(line);
				contentList.add(line);
			}
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + path);
		} catch (IOException e) {
			System.out.println("Unable to read file: " + path);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				System.out.println("Unable to close file: " + path);
			} catch (NullPointerException ex) {
				// File was probably never opened!
			}
		}

		this.root = VfdtNode.buildModelFromFile(contentListLength - 1, contentList, nbFeatureValues);
	}

	/**
	 * Return the visualization of the tree.
	 *
	 * DO NOT CHANGE THIS METHOD.
	 *
	 * @return Visualization of the tree
	 */
	public String getVisualization() {
		return root.getVisualization("");
	}

	/**
	 * This runs your code to generate the required output for the assignment.
	 * 
	 * DO NOT CHANGE THIS METHOD.
	 */
	public static void main(String[] args) {
		if (args.length < 7) {
			System.err.println(
					"Usage: java Vfdt <delta> <tau> <nmin> <data set> <nbFeatureValues> <output file> <reportingPeriod> [-writeOutAllPredictions]");
			throw new Error("Expected 7 or 8 arguments, got " + args.length + ".");
		}
		try {
			// parse input
			double delta = Double.parseDouble(args[0]);
			double tau = Double.parseDouble(args[1]);
			int nmin = Integer.parseInt(args[2]);
			Data<Integer> data = new IntData(args[3], ",");
			int[] nbFeatureValues = parseNbFeatureValues(args[4]);
			String out = args[5];
			int reportingPeriod = Integer.parseInt(args[6]);
			boolean writeOutAllPredictions = args.length > 7 && args[7].contains("writeOutAllPredictions");

			// initialize learner
			Vfdt vfdt = new Vfdt(nbFeatureValues, delta, tau, nmin);

			// generate output for the learning curve
			vfdt.makeLearningCurve(data, 0.5, out + ".vfdt", reportingPeriod, writeOutAllPredictions);

		} catch (IOException e) {
			System.err.println(e.toString());
		}
	}

	/**
	 * This method parses the file that specifies the nb of possible values for
	 * each feature.
	 * 
	 * DO NOT CHANGE THIS METHOD.
	 */
	private static int[] parseNbFeatureValues(String path) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(path));
		reader.readLine(); // skip header
		String[] splitLine = reader.readLine().split(",");
		int[] nbFeatureValues = new int[splitLine.length];

		for (int i = 0; i < nbFeatureValues.length; i++) {
			nbFeatureValues[i] = Integer.parseInt(splitLine[i]);
		}
		reader.close();
		return nbFeatureValues;
	}

	public String getInfo() {
		// return this.getVisualization();
		return new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	}

}

/**
 * This class implements Data for Integers
 *
 * DO NOT CHANGE THIS CLASS
 *
 */
class IntData extends Data<Integer> {

	public IntData(String dataDir, String sep) throws FileNotFoundException {
		super(dataDir, sep);
	}

	@Override
	protected Integer parseAttribute(String attrString) {
		return Integer.parseInt(attrString);
	}

	@Override
	protected Integer[] emptyAttributes(int i) {
		return new Integer[i];
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			throw new Error("Expected 2 arguments, got " + args.length + ".");
		}

		try {
			Data<Integer> d = new IntData(args[0], args[1]);
			d.print();
		} catch (FileNotFoundException e) {
			System.err.print(e.toString());
		}
	}
}
