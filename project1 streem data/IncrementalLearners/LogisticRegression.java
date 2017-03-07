
/**
   @author Bo Wang

   This class is a stub for incrementally building a Logistic Regression model. 

   (c) 2016
 */
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.List;

public class LogisticRegression extends IncrementalLearner<Double> {

	private double learningRate;
	private double regularizationParameter;
	private double[] weights;

	/**
	 * LogisticRegression constructor.
	 * 
	 * @param numFeatures
	 *            is the number of features.
	 * @param learningRate
	 *            is the learning rate
	 */
	public LogisticRegression(int numFeatures, double learningRate, double regularizationParameter) {
		this.learningRate = learningRate;
		this.regularizationParameter = regularizationParameter;
		nbExamplesProcessed = 0;
		// init the weight array
		this.weights = new double[numFeatures + 1];
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
	public void update(Example<Double> example) {
		super.update(example);

		double logit = 0.0;
		for (int i = 0; i < example.attributeValues.length; i++) {
			logit += example.attributeValues[i] * this.weights[i + 1];
		}

		logit += this.weights[0];
		double propYis1 = 1 / (1 / Math.exp(logit) + 1);

		// update W0.
		this.weights[0] = this.weights[0] + this.learningRate * (example.classValue - propYis1);
		// update W1...Wn.
		for (int i = 1; i < this.weights.length; i++) {
			this.weights[i] = this.weights[i] + this.learningRate * (-this.regularizationParameter * this.weights[i]
					+ example.attributeValues[i - 1] * (example.classValue - propYis1));
		}

	}

	/**
	 * Uses the current model to calculate the probability that an
	 * attributeValues belongs to class "1";
	 * 
	 * 
	 * @param example
	 *            is a test attributeValues
	 * @return the probability that attributeValues belongs to class "1"
	 */
	@Override
	public double makePrediction(Double[] example) {
		double logit = 0;
		for (int i = 0; i < example.length; i++) {
			logit += example[i] * this.weights[i + 1];
		}
		logit += this.weights[0];
		double propYis1 = 1 / (1 / Math.exp(logit) + 1);
		return propYis1;
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

		PrintWriter modelWriter = new PrintWriter(path);
		String formatedString = Arrays.toString(this.weights).replace(",", "") // remove
																				// the
																				// commas
				.replace("[", "") // remove the right bracket
				.replace("]", "") // remove the left bracket
				.trim();

		modelWriter.println(formatedString);
		modelWriter.flush();
		modelWriter.close();
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
		try {
			reader = new BufferedReader(new FileReader(path));
			// read the first line

			String firstline = reader.readLine();
			if (firstline != null) {
				double[] tempArray = Arrays.stream(firstline.split(" ")).map(String::trim)
						.mapToDouble(Double::parseDouble).toArray();

				this.weights = tempArray;
			} else {// empty file
				System.out.println("The model file is an empty file.");
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

		/**
		 * // create Scanner inFile1 this.nbExamplesProcessed =
		 * nbExamplesProcessed; Scanner inFile1 = new Scanner(new File(path));
		 * List<Double> temps = new ArrayList<Double>(); while
		 * (inFile1.hasNext()) { double token1 = inFile1.nextDouble();
		 * temps.add(token1); } inFile1.close(); //Here to change this.weights =
		 * temps.toArray(new Double[0]);
		 */
	}

	/**
	 * This runs your code to generate the required output for the assignment.
	 *
	 * DO NOT CHANGE THIS METHOD
	 *
	 */
	public static void main(String[] args) {
		if (args.length < 5) {
			System.err.println(
					"Usage: java LogisticRegression <learningRate> <regularizationParameter> <data set> <output file> <reportingPeriod> [-writeOutAllPredictions]");
			throw new Error("Expected 5 or 6 arguments, got " + args.length + ".");
		}
		try {
			// parse input
			double learningRate = Double.parseDouble(args[0]);
			double regularizationParameter = Double.parseDouble(args[1]);
			DoubleData data = new DoubleData(args[2], ",");
			String out = args[3];
			int reportingPeriod = Integer.parseInt(args[4]);
			boolean writeOutAllPredictions = args.length > 5 && args[5].contains("writeOutAllPredictions");

			// initialize learner
			LogisticRegression lr = new LogisticRegression(data.getNbFeatures(), learningRate, regularizationParameter);

			// generate output for the learning curve
			lr.makeLearningCurve(data, 0.5, out + ".lr", reportingPeriod, writeOutAllPredictions);

		} catch (FileNotFoundException e) {
			System.err.println(e.toString());
		}
	}
}

/**
 * This class implements Data for Doubles
 *
 * DO NOT CHANGE THIS CLASS
 *
 */
class DoubleData extends Data<Double> {

	public DoubleData(String dataDir, String sep) throws FileNotFoundException {
		super(dataDir, sep);
	}

	@Override
	protected Double parseAttribute(String attrString) {
		return Double.parseDouble(attrString);
	}

	@Override
	protected Double[] emptyAttributes(int i) {
		return new Double[i];
	}
}