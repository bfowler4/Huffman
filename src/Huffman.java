import java.util.Collections;
import java.util.LinkedList;

/**
 * Methods below are used to compress or uncompress a file using huffman coding.
 * Main method is contained in the "Driver" class.
 * 
 * @author Brandon Fowler
 *
 */
public class Huffman {
	// Flag to force compression if compressed file is not smaller than
	// uncompressed
	private boolean force = false;

	// Flag to provide debug information such as huff tree, file size before and
	// after compression, frequency table, and huff codes
	private boolean verbose = false;

	private boolean compress = false;
	private boolean uncompress = false;
	private String inputFile = "";
	private String outputFile = "";
	private int[] frequencies = new int[256];
	private String[] lookupTable = new String[256];
	private HuffNode tree;
	private int numChars;
	private int numUniqueChars;
	private int originalSize;
	private int newSize;
	private boolean oneChar = false;

	public Huffman(String[] args) {
		parseArguments(args);
		doHuffman();
	}

	/**
	 * Used to read in command line arguments. Sets boolean values depending on
	 * given flags.
	 * 
	 * @param args
	 *            - command line arguments
	 */
	private void parseArguments(String[] args) {
		if (args[0].equals("-c") || args[0].equals("-C")) {
			compress = true;
		} else {
			uncompress = true;
		}

		inputFile = args[args.length - 2];
		outputFile = args[args.length - 1];

		for (int i = 1; i < args.length - 2; i++) {
			if (args[i].equals("-v") || args[i].equals("-V")) {
				verbose = true;
			} else if (args[i].equals("-f") || args[i].equals("-F")) {
				force = true;
			}
		}
	}

	/**
	 * Used during debugging to check parseArguments and the status of each flag
	 */
	private void printStatus() {
		System.out.println("Compress: " + compress);
		System.out.println("Uncompress: " + uncompress);
		System.out.println("Force: " + force);
		System.out.println("Verbose: " + verbose);
		System.out.println("Input File: " + inputFile);
		System.out.println("Output File: " + outputFile);
	}

	/**
	 * Goes through file and determine frequencies of each char in the file
	 */
	private void getFrequencies() {
		TextFile textFile = new TextFile(inputFile, 'r');
		while (!textFile.EndOfFile()) {
			char nextChar = textFile.readChar();
			frequencies[nextChar] += 1;
			numChars += 1;
		}

		textFile.close();
	}

	/**
	 * Used to build the huff tree from the frequency array
	 */
	private void buildHuffTreeFromFrequencies() {
		LinkedList preHuffTree = new LinkedList<HuffNode>();

		// adds original frequencies to a linked list
		for (int i = 0; i < frequencies.length; i++) {
			if (frequencies[i] != 0) {
				preHuffTree.add(new HuffNode(frequencies[i], i, null, null));
				numUniqueChars++;
			}
		}

		// used to determine number of node combines needed
		int size = preHuffTree.size();

		if (size == 1) {
			oneChar = true;
		}

		for (int i = 0; i < size - 1; i++) {
			Collections.sort(preHuffTree);

			HuffNode leftNode = (HuffNode) preHuffTree.removeFirst();
			HuffNode rightNode = (HuffNode) preHuffTree.removeFirst();
			HuffNode parentNode = new HuffNode(leftNode.freq + rightNode.freq,
					-1, leftNode, rightNode);
			preHuffTree.addFirst(parentNode);
		}

		tree = (HuffNode) preHuffTree.get(0);
	}

	/**
	 * Used to call recursive method of buildLookupTable(). If there is only one
	 * unique character in the text file, sets the character code to "0". This
	 * is a special case.
	 */
	private void buildLookupTable() {
		// Checks for special case of only one unique character
		if (oneChar) {
			lookupTable[tree.ascii] = "0";
		} else {
			buildLookupTable(tree, "");
		}
	}

	/**
	 * Used to insert codes into the lookup table for each char in the file
	 * 
	 * @param tree
	 *            - Huffman tree
	 * @param code
	 *            - code for letter as tree is being traversed
	 */
	private void buildLookupTable(HuffNode tree, String code) {
		if (tree.left == null && tree.right == null) {
			lookupTable[tree.ascii] = code;
			return;
		}
		buildLookupTable(tree.left, code + 0);
		buildLookupTable(tree.right, code + 1);
	}

	/**
	 * Determines if compressed file will be smaller than the original file
	 * 
	 * @return - true if file is smaller when compressed, false if not
	 */
	private boolean compareCompress() {
		// original file size
		originalSize = numChars * 8;

		// 16 bits for magic number and 32 bits for header = 16 + 32 48
		newSize = 48;

		// add size of tree
		newSize += numUniqueChars * 9; // 9 bits per leaf
		newSize += numUniqueChars - 1; // 1 bit per internal node

		// frequency of each char multiplied by its huffman code
		for (int i = 0; i < lookupTable.length; i++) {
			if (lookupTable[i] != null) {
				newSize = newSize + (frequencies[i] * lookupTable[i].length());
			}
		}

		// make sure size of compressed file is a multiple of 8
		if (newSize % 8 != 0) {
			int size = newSize / 8; // remove the left over bits
			newSize = size * 8 + 8; // add 8 to round the bits up
		}

		return originalSize > newSize;

	}

	/**
	 * Method to write the magic number, huffman tree, and the encoded file to
	 * the given output file.
	 */
	private void writeBinaryToFile() {
		BinaryFile binaryFile = new BinaryFile(outputFile, 'w');
		TextFile textFile = new TextFile(inputFile, 'r');
		textFile.rewind();

		// magic number
		binaryFile.writeChar('H');
		binaryFile.writeChar('F');

		writeTreeToFile(tree, binaryFile);
		encodeFile(textFile, binaryFile);

		textFile.close();
		binaryFile.close();
	}

	/**
	 * Method to encode file. Loops through the given input file and determines
	 * the code for each character. Writes code to the given output file.
	 * 
	 * @param textFile
	 *            - text file to be read from
	 * @param binaryFile
	 *            - output binary file to be written to
	 */
	private void encodeFile(TextFile textFile, BinaryFile binaryFile) {
		while (!textFile.EndOfFile()) {
			char nextChar = textFile.readChar();
			String code = lookupTable[nextChar];
			for (int i = 0; i < code.length(); i++) {
				if (code.charAt(i) == '0') {
					binaryFile.writeBit(false);
				} else {
					binaryFile.writeBit(true);
				}
			}
		}
	}

	/**
	 * Method used to write huff tree to binary file
	 * 
	 * @param tree
	 *            - huff tree
	 * @param binaryFile
	 *            -file to be written to
	 */
	private void writeTreeToFile(HuffNode tree, BinaryFile binaryFile) {
		if (tree.left == null && tree.right == null) {
			binaryFile.writeBit(true);
			binaryFile.writeChar(tree.ascii);
		} else {
			binaryFile.writeBit(false);
			writeTreeToFile(tree.left, binaryFile);
			writeTreeToFile(tree.right, binaryFile);
		}
	}

	/**
	 * Calls necessary methods to decode specified file.
	 */
	private void decodeFile() {
		BinaryFile binaryFile = new BinaryFile(inputFile, 'r');
		TextFile textFile = new TextFile(outputFile, 'w');

		char m1 = binaryFile.readChar();
		char m2 = binaryFile.readChar();

		if (m1 != 'H' || m2 != 'F') {
			System.out.println("File provided does not contain magic number!");
			System.exit(0);
		}

		HuffNode tree = buildHuffTreeFromFile(binaryFile);
		this.tree = tree;

		// Only 1 unique char, tree only has 1 node so must check for special
		// case
		if (tree.left == null && tree.right == null) {
			while (!binaryFile.EndOfFile()) {
				binaryFile.readBit();
				textFile.writeChar(tree.ascii);
			}
		} else {
			while (!binaryFile.EndOfFile()) {
				decodeToFile(tree, binaryFile, textFile);
			}
		}
		binaryFile.close();
		textFile.close();
	}

	/**
	 * Builds the huffman tree from the given file.
	 * 
	 * @param binaryFile
	 *            - binary file that contains the tree
	 * @return - returns new huff node that has been added to the tree
	 */
	private HuffNode buildHuffTreeFromFile(BinaryFile binaryFile) {
		boolean isLeaf = binaryFile.readBit();
		if (isLeaf) {
			char nextChar = binaryFile.readChar();
			return new HuffNode(0, nextChar, null, null);
		} else {
			return new HuffNode(0, -1, buildHuffTreeFromFile(binaryFile),
					buildHuffTreeFromFile(binaryFile));
		}
	}

	/**
	 * Decodes given file to the specified output file. Does not read in the
	 * huff tree or check for the magic number. Other methods for that.
	 * 
	 * @param tree
	 *            - Huff tree
	 * @param binaryFile
	 *            - binary file to decode
	 * @param textFile
	 *            - text file where decoded file will be written to
	 */
	private void decodeToFile(HuffNode tree, BinaryFile binaryFile,
			TextFile textFile) {
		if (tree.left == null && tree.right == null) {
			textFile.writeChar(tree.ascii);
		} else {
			if (binaryFile.readBit())
				decodeToFile(tree.right, binaryFile, textFile);
			else
				decodeToFile(tree.left, binaryFile, textFile);
		}

	}

	/**
	 * Main method which will compress or uncompress depending on the command
	 * line arguments. Will also print out information if verbose flag is
	 * provided.
	 */
	private void doHuffman() {
		boolean isEmpty = isFileEmpty();
		if (isEmpty) {
			System.out
					.println("Input file provided contains no data to compress or uncompress.");
			System.exit(0);
		} else if (compress) {
			getFrequencies();
			buildHuffTreeFromFrequencies();
			buildLookupTable();
			if (!force) {
				if (compareCompress()) {

				} else {
					System.out
							.println("File was not compressed due to compressed file being larger than the original file.");
					System.exit(0);
				}
			}
			writeBinaryToFile();
			if (verbose) {
				// Needed to determine size of original file and compressed file
				if (force)
					compareCompress();

				// Prints out frequency table
				System.out.println("Frequency table -");
				for (int i = 0; i < frequencies.length; i++) {
					System.out.println(i + ": " + frequencies[i]);
				}
				System.out.println();

				// Prints out Huffman Tree
				System.out.println("Huffman Tree -");
				printTree(tree, 0);
				System.out.println();
				System.out.println();

				// Prints out Huffman Codes
				System.out.println("Huffman Codes -");
				for (int i = 0; i < lookupTable.length; i++) {
					if (lookupTable[i] != null) {
						System.out.println(i + ": " + lookupTable[i]);
					}
				}
				System.out.println();

				// Prints out original and compressed file sizes
				System.out.println("Original Size: " + originalSize);
				System.out.println("Compressed Size: " + newSize);
				System.out.println();
			}
		} else {
			decodeFile();
			if (verbose) {
				System.out.println("Huffman Tree -");
				printTree(tree, 0);
			}
		}
	}

	/**
	 * Used to print huff tree to the console if verbose flag is provided. Will
	 * print a "@" to represent the inner nodes
	 * 
	 * @param tree
	 *            - huff tree
	 */
	public static void printTree(HuffNode tree, int offset) {
		if (tree.left == null && tree.right == null) {
			for (int i = 0; i < offset; i++) {
				System.out.print(" ");
			}
			System.out.print((int) tree.ascii);
			System.out.println();
		} else {
			for (int i = 0; i < offset; i++) {
				System.out.print(" ");
			}
			System.out.print("@");
			System.out.println();
			printTree(tree.left, offset + 1);
			printTree(tree.right, offset + 1);
		}
	}

	/**
	 * Checks if provided input file is empty.
	 * 
	 * @return - returns true if file is empty, false if not
	 */
	private boolean isFileEmpty() {
		if (compress) {
			TextFile textFile = new TextFile(inputFile, 'r');
			if (textFile.EndOfFile()) {
				textFile.close();
				return true;
			}
			textFile.close();
			return false;
		} else {
			BinaryFile binaryFile = new BinaryFile(inputFile, 'r');
			if (binaryFile.EndOfFile()) {
				binaryFile.close();
				return true;
			}
			binaryFile.close();
			return false;
		}
	}

	private class HuffNode implements Comparable<HuffNode> {
		private char ascii;
		private int freq;
		private HuffNode right = null;
		private HuffNode left = null;

		public HuffNode(int freq, int ascii, HuffNode left, HuffNode right) {
			this.freq = freq;
			this.ascii = (char) ascii;
			this.right = right;
			this.left = left;
		}

		/**
		 * Used for sorting huff nodes when building huff tree from frequencies.
		 */
		@Override
		public int compareTo(HuffNode other) {
			return Integer.compare(freq, other.freq);
		}

	}

}
