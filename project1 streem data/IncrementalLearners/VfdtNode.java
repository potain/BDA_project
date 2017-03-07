import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Bo Wang
 * 
 *         This class is a stub for VFDT.
 * 
 *         (c) 2016
 */

public class VfdtNode {

	private VfdtNode[] children; /* children (null if node is a leaf) */
	/*
	 * The features that this node can split on
	 */
	private final int[] possibleSplitFeatures;

	private int splitFeature; /* splitting feature */

	private int[][][] nijk; /* instance counts (see paper) */

	/* FILL IN HERE */
	/**
	 * counter to count if the example collect larger than nmin. If after
	 * evaluation, the node decided not to split, then this value been reset to
	 * 0.
	 *
	 */
	private int exampleCounter = 0;
	/**
	 * The number of example collected under the leaf since the splits, this
	 * value was used to calculate epson.
	 */
	private int nrExamplesTotal = 0;

	/**
	 * number of examples belongs to class1 and class2
	 */
	private int[] nrClassCount = new int[2];

	private int[] nbFeatureValues;

	/**
	 * Create and initialize a leaf node.
	 * 
	 * 
	 * @param nbFeatureValues
	 *            are the nb of values for each feature in this node. If a
	 *            feature has k values, then the values are [0:k-1].
	 */
	public VfdtNode(int[] nbFeatureValues, int[] possibleSplitFeatures) {
		this.nbFeatureValues = nbFeatureValues;
		this.possibleSplitFeatures = possibleSplitFeatures;
		children = null;
		this.nijk = new int[nbFeatureValues.length][][];
		for (int i : possibleSplitFeatures) {
			int[][] anon = new int[nbFeatureValues[i]][2];
			for (int j = 0; j < nbFeatureValues[i]; j++) {
				anon[j][0] = 0;
				anon[j][1] = 0;
			}
			this.nijk[i] = anon;
		}
	}

	/**
	 * Turn a leaf node into a internal node.
	 * 
	 * 
	 * @param splitFeature
	 *            is the feature to test on this node.
	 * @param nodes
	 *            are the children (the index of the node is the value of the
	 *            splitFeature).
	 */
	public void addChildren(int splitFeature, VfdtNode[] nodes) {
		if (nodes == null)
			throw new IllegalArgumentException("null children");
		// nbSplits++;

		this.children = nodes;
		this.splitFeature = splitFeature;

		// step4 free resources
		this.nijk = null; // free resource
		this.nrClassCount = null;
	}

	/**
	 * Returns the leaf node corresponding to the test attributeValues.
	 * 
	 * 
	 * @param example
	 *            is the test attributeValues to sort.
	 */
	public VfdtNode sortExample(Integer[] example) {
		if (children == null) {
			return this;
		} else {
			return children[example[this.splitFeature]].sortExample(example);
		}

	}

	/**
	 * Split evaluation method (function G in the paper)
	 * 
	 * Compute a splitting score for the feature featureId. For now, we'll use
	 * information gain, but this may be changed. You can test your code with
	 * other split evaluations, but be sure to change it back to information
	 * gain in the submitted code and for the experiments with default values.
	 * 
	 * @param featureId
	 *            is the feature to be considered.
	 */
	public double splitEval(int featureId) {
		return informationGain(featureId, nijk);
	}

	/**
	 * Compute the information gain of a feature for this leaf node.
	 * 
	 * 
	 * @param featureId
	 *            is the feature to be considered.
	 * @param nijk
	 *            are the instance counts.
	 */
	public static double informationGain(int featureId, int[][][] nijk) {
		double ig = 0;

		// we do not use this.nrClassCount[0] and this.ClassCount[1] because we
		// need to use the new cummulated samples
		int nrClass0 = 0;
		int nrClass1 = 0;
		int featureValuesSetLen = nijk[featureId].length;
		for (int j = 0; j < featureValuesSetLen; j++) {
			nrClass0 += nijk[featureId][j][0];
			nrClass1 += nijk[featureId][j][1];
		}
		int nrTotal = nrClass0 + nrClass1;

		if (nrTotal == 0) {
			return 0.0;
		} else {
			double entropyBeforeSplit = calEntropy(nrClass1, nrClass0);

			double entropyAfterSplit = 0.0;
			for (int j = 0; j < featureValuesSetLen; j++) {
				double numberJ = (nijk[featureId][j][0] + nijk[featureId][j][1]) * 1.0;
				entropyAfterSplit += numberJ / nrTotal * calEntropy(nijk[featureId][j][1], nijk[featureId][j][0]);
			}
			ig = entropyBeforeSplit - entropyAfterSplit;
			return ig;
		}
	}

	private static double calEntropy(int nrClass1, int nrClass0) {
		int nrTotal = nrClass0 + nrClass1;
		if (nrClass0 == 0 || nrClass1 == 0) {
			return 0;
		} else {
			double P = nrClass1 * 1.0 / nrTotal;
			double N = nrClass0 * 1.0 / nrTotal;
			return -P * Math.log(P) / Math.log(2) - N * Math.log(N) / Math.log(2);
		}
	}

	/**
	 * Update the nijk value.
	 * 
	 * @param example
	 */
	public void updateExample(Example<Integer> example, int nmin, double delta, double tau) {

		VfdtNode leaf = sortExample(example.attributeValues);
		leaf.exampleCounter += 1;
		leaf.nrExamplesTotal += 1;
		leaf.nrClassCount[example.classValue] += 1;

		for (int i : leaf.possibleSplitFeatures) {
			leaf.nijk[i][example.attributeValues[i]][example.classValue] += 1;
		}

		if (leaf.exampleCounter >= nmin) {
			double[] infoGainArray = new double[leaf.possibleSplitFeatures.length];
			for (int i = 0; i < leaf.possibleSplitFeatures.length; i++) {
				infoGainArray[i] = leaf.splitEval(leaf.possibleSplitFeatures[i]);
			}
			double epson = Math.sqrt(Math.log(2 / delta) / (2 * leaf.nrExamplesTotal));

			// Sort out maxOne and maxTwo index
			int max1Index = 0;
			int max2Index = 0;
			for (int i = 0; i < infoGainArray.length; i++) {
				if (infoGainArray[max1Index] < infoGainArray[i]) {
					max2Index = max1Index;
					max1Index = i;
				} else if (infoGainArray[max2Index] < infoGainArray[i]) {
					max2Index = i;
				}
			}

			double max1 = infoGainArray[max1Index];
			double max2 = infoGainArray[max2Index];

			if ((max1 - max2) > epson || ((max1 - max2) < epson && (max1 - max2) < tau && (epson < tau))) {
				// crate children nodes and split the leaf
				int splitFeature = leaf.possibleSplitFeatures[max1Index];
				VfdtNode[] childNodes = new VfdtNode[this.nbFeatureValues[splitFeature]];

				// step1 creat new possibleSplitFeatures //to do maybe use
				// another data structure.
				int[] newPossibleSplitFeatures = new int[leaf.possibleSplitFeatures.length - 1];
				System.arraycopy(leaf.possibleSplitFeatures, 0, newPossibleSplitFeatures, 0, max1Index);
				System.arraycopy(leaf.possibleSplitFeatures, max1Index + 1, newPossibleSplitFeatures, max1Index,
						leaf.possibleSplitFeatures.length - max1Index - 1);

				// step2 creat childnodes
				for (int i = 0; i < this.nbFeatureValues[splitFeature]; i++) {
					VfdtNode node = new VfdtNode(this.nbFeatureValues, newPossibleSplitFeatures);
					node.nrClassCount[0] = leaf.nijk[splitFeature][i][0];
					node.nrClassCount[1] = leaf.nijk[splitFeature][i][1];
					childNodes[i] = node;
				}

				// step3 split leaf
				leaf.addChildren(splitFeature, childNodes);
			} else {// need to collect more examples
				leaf.exampleCounter = 0;
			}
			;

		}

	}

	/**
	 * Make prediction based on the example.
	 * 
	 * @param example
	 * @return
	 */
	public double makePrediction(Integer[] example) {
		VfdtNode leaf = sortExample(example);
		int nrTotal = leaf.nrClassCount[0] + leaf.nrClassCount[1];
		if (nrTotal == 0) {// No observation
			return 0.5;
		} else {
			return leaf.nrClassCount[1] * 1.0 / nrTotal;
		}

	}

	/**
	 * Return the visualization of the tree.
	 *
	 * DO NOT CHANGE THIS METHOD.
	 *
	 * @return Visualization of the tree
	 */
	public String getVisualization(String indent) {
		if (children == null) {
			return indent + "Leaf\n";
		} else {
			String visualization = "";
			for (int v = 0; v < children.length; v++) {
				visualization += indent + splitFeature + "=" + v + ":\n";
				visualization += children[v].getVisualization(indent + "| ");
			}
			return visualization;
		}
	}

	/**
	 * 
	 * @param nbFeatureValues
	 * @return The String represent the model that going to be written to the
	 *         file.
	 */
	public String generateModelString(int[] nbFeatureValues) {
		NodeString model = this.generateModelStringIntermediate(nbFeatureValues, 0);
		String nodesNumber = Integer.toString(model.index);
		return nodesNumber + "\n" + model.nodeContent;
	}

	/**
	 * 
	 * @param nbFeatureValues
	 * @param lineNr
	 *            the line number that this node going to output.
	 * @return The NodeString class represent the model.
	 */
	private NodeString generateModelStringIntermediate(int[] nbFeatureValues, int lineNr) {
		NodeString mNode = new NodeString();
		mNode.nodeContent = "";
		if (this.children == null) {// this is a leaf
			NodeString leaf = new NodeString();
			List<String> nijkStringList = new ArrayList<>();
			String leafString;
			for (int i : this.possibleSplitFeatures)
				for (int j = 0; j < nbFeatureValues[i]; j++)
					for (int k = 0; k < 2; k++) {
						if (this.nijk[i][j][k] != 0) {
							nijkStringList.add(String.format("%d:%d:%d:%d", i, j, k, this.nijk[i][j][k]));
						}
					}
			String tempStr = Arrays.toString(this.possibleSplitFeatures);
			if (nijkStringList.size() != 0) {
				leafString = Integer.toString(lineNr) + " L " + "pf:"
						+ tempStr.substring(0, tempStr.length() - 1).replace(" ", "") + ",]" + " nijk:["
						+ String.join(",", nijkStringList) + ",]" + "\n";
			} else {
				leafString = Integer.toString(lineNr) + " L " + "pf:"
						+ tempStr.substring(0, tempStr.length() - 1).replace(" ", "") + ",]" + " nijk:[]" + "\n";
			}
			leaf.index = lineNr + 1;// go to the next line
			leaf.nodeContent = leafString;
			return leaf;
		} else {// this is a node
			int[] childArray = new int[children.length];// Store child line
														// number.
			for (int v = 0; v < children.length; v++) {
				NodeString tempLeafNode = children[v].generateModelStringIntermediate(nbFeatureValues, mNode.index);
				mNode.nodeContent += tempLeafNode.nodeContent;
				mNode.index = tempLeafNode.index;
				// System.out.println("mNode.index:" + mNode.index);
				childArray[v] = mNode.index - 1;
			}
			String tempStr = Arrays.toString(childArray);
			String nodeString = Integer.toString(mNode.index) + " D " + "f:" + this.splitFeature + " " + "ch:"
					+ tempStr.substring(0, tempStr.length() - 1).replace(" ", "") + ",]" + "\n";
			mNode.nodeContent += nodeString;
			mNode.index += 1;// go to the next line
			return mNode;
		}
	}

	/**
	 * 
	 * @param i
	 *            The number of line to start read from.
	 * @param contentList
	 *            The List representation of the model file. Each line is a
	 *            element of the List.
	 * @param nbFeatureValues
	 * @return The VfdtNode that represent the Model root.
	 */
	public static VfdtNode buildModelFromFile(int i, List<String> contentList, int[] nbFeatureValues) {
		String[] toparser = contentList.toArray(new String[0])[i].split(" ");
		if (toparser[1].equals("L")) {// this is a leaf
			String str1 = toparser[2].split(":")[1];// "[1, 2, 3, 4, 5, 6, 7, 8,
													// 9, 0]";
			int[] possibleFeatures = Arrays.stream(str1.substring(1, str1.length() - 2).split(",")).map(String::trim)
					.mapToInt(Integer::parseInt).toArray();

			VfdtNode leaf = new VfdtNode(nbFeatureValues, possibleFeatures);

			String tempStr = toparser[3];
			String str2 = tempStr.substring(5, tempStr.length());// "[0:1:1:1,0:2:0:2,1:1:1:1,1:0:0:2,]";
			String[] nijkStringArray = new String[] {};
			if (str2.length() > 2) {
				nijkStringArray = str2.substring(1, str2.length() - 2).split(",");
			}

			for (String temp : nijkStringArray) {
				int[] nijkInt = Arrays.stream(temp.split(":")).map(String::trim).mapToInt(Integer::parseInt).toArray();
				leaf.nijk[nijkInt[0]][nijkInt[1]][nijkInt[2]] = nijkInt[3];
			}

			int testF = leaf.possibleSplitFeatures[0];
			for (int j = 0; j < nbFeatureValues[testF]; j++) {
				leaf.nrClassCount[0] += leaf.nijk[testF][j][0];
				leaf.nrClassCount[1] += leaf.nijk[testF][j][1];
			}

			return leaf;
		} else {
			int[] possibleFeatures = new int[nbFeatureValues.length];
			for (int m = 0; m < nbFeatureValues.length; m++)
				possibleFeatures[m] = m;
			VfdtNode node = new VfdtNode(nbFeatureValues, possibleFeatures);
			int splitFeature = Integer.parseInt(toparser[2].split(":")[1]);

			String str1 = toparser[3].split(":")[1];// "[1,2,3,4]";
			int[] childIndex = Arrays.stream(str1.substring(1, str1.length() - 2).split(",")).map(String::trim)
					.mapToInt(Integer::parseInt).toArray();
			VfdtNode[] children = new VfdtNode[childIndex.length];
			for (int k = 0; k < childIndex.length; k++) {
				children[k] = buildModelFromFile(childIndex[k], contentList, nbFeatureValues);
			}

			node.addChildren(splitFeature, children);
			return node;
		}

	}

}

/**
 * Represent content of the node and the number of sub nodes.
 * 
 * @author wangbo
 *
 */
class NodeString {
	public int index;
	public String nodeContent;
}
