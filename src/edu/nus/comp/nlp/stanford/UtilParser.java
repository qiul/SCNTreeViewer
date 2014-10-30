/**
* A Stanford CoreNLP tree visualizer. 
* Copyright (C) 2014  Long Qiu

* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.

* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.

* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

package edu.nus.comp.nlp.stanford;

/*
 * The Dynamic Tree part is based on an example provided by Richard Stanford,
 * a tutorial reader.
 */


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

public class UtilParser {
	private static StanfordCoreNLP pipeline;
	static TreeFactory lstf = new LabeledScoredTreeFactory();
	private static final String ANALYZERS  ="tokenize, ssplit, pos, lemma, parse"; 
	static Properties props;
	
	
	public static void main(String[] args){
		String input = null;
		boolean showed = false;
		if(args==null || args.length ==0){
			showHelpMessage();	
			showed = true;
		}

		for(int i=0; i<args.length; i++){
			if(args[i].equals("-h") || args[i].equals("--help") ){
				showHelpMessage();	
				System.exit(0);
			}
			if(args[i].equals("-example")){
				showDepTree("Here are some example sentences for SCNTreeViewer. There are a few symbols in Scala that are special and cannot be defined or used as method names. You have to understand this.");
				showed = true;
			}
			
			if(args[i].equals("-t") && i+1<args.length){
				input = args[++i];
			}
			
			if(args[i].equals("-f") && i+1<args.length){
				input = UtilParser.readFile(args[++i], null).toString();//read file
			}
		}
		if(!showed && input != null){
			showDepTree(input);
		}else{
			System.out.println("The viewer didn't get any valid input.");	
		}
	}
	
	private static void lazyInit(){
		if(props != null){
			return;
		}
		props = new Properties();
		//No effect. Still can't recognize it
		//props.put("pos.model", "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");

		props.put("annotators", ANALYZERS);
		pipeline = new StanfordCoreNLP(props); 
		System.err.println("Loading done.");
	}

	/**
	 * Copied from Util.java
	 * @param fileName
	 * @param commentFlag
	 * @return
	 */
	 private static StringBuffer readFile(String fileName, String commentFlag) {
		    StringBuffer sb = new StringBuffer();
		    try {
		      BufferedReader in =
		          new BufferedReader(new FileReader(fileName));
		      String s;
		      while ( (s = in.readLine()) != null) {
		        if (commentFlag != null
		            && s.trim().startsWith(commentFlag)) {
		          //ignore this line
		          continue;
		        }
		        sb.append(s);
		        //sb.append("/n"); to make it more platform independent (Log July 12, 2004)
		        sb.append(System.getProperty("line.separator"));
		      }
		      in.close();
		    }
		    catch (IOException ex) {
		      ex.printStackTrace();
		    }
		    return sb;
		  }


	public static Annotation parseArticle(String text){
		Annotation document = new Annotation(text);
		// run all Annotators on this text
		pipeline.annotate(document);
		return document;
	}

	public static ArrayList<Tree> getParseTree(Annotation document){
		ArrayList<Tree> forest = new ArrayList<Tree>();
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
//			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
//				// this is the text of the token
//				String word = token.get(TextAnnotation.class);
//				// this is the POS tag of the token
//				String pos = token.get(PartOfSpeechAnnotation.class);
//				// this is the NER label of the token
//				String ne = token.get(NamedEntityTagAnnotation.class);       
//			}

			// this is the parse tree of the current sentence
			Tree tree = sentence.get(TreeAnnotation.class); 
			// Alternatively, this is the Stanford dependency graph of the current sentence, but without punctuations
//			SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
			forest.add(tree);
		}
		return forest;
	}
	
	public static Tree getDepTree(Tree parseTree){
		//Dependencies, punctuations included
		GrammaticalStructure gs = new EnglishGrammaticalStructure(parseTree, new PennTreebankLanguagePack().punctuationWordAcceptFilter());
		Collection<TypedDependency> tdl = gs.typedDependencies();  // typedDependenciesCollapsed() eats the prepositions, etc
		gs = new EnglishGrammaticalStructure(parseTree);
		tdl.addAll(gs.typedDependencies());
		Tree depTree = makeTreeRobust(tdl);
		return depTree;
	}
	
	public static ArrayList<Tree> getDepForest(ArrayList<Tree> parseForest){
		ArrayList<Tree> depForest = new ArrayList<Tree>();
		for(Tree tree: parseForest){
			Tree depTree = getDepTree(tree);
			depForest.add(depTree);
		}
		return depForest;
	}
	
	public static void showDepTree(String text){
		if(props == null){
			lazyInit();
		}
		Annotation document  = parseArticle(text);
		ArrayList<Tree> parseTrees = getParseTree(document);
		ArrayList<Tree> depForest = getDepForest(parseTrees);

		DefaultMutableTreeNode rootParse = new DefaultMutableTreeNode();
		for(int i=0; i<parseTrees.size(); i++){
			rootParse.add(toDMTree(null, parseTrees.get(i)));
		}
		DefaultMutableTreeNode rootDep = new DefaultMutableTreeNode();
		for(int i=0; i<parseTrees.size(); i++){
			rootDep.add(toDMTree(null, depForest.get(i)));
		}
		DefaultMutableTreeNode[] trees = new DefaultMutableTreeNode[2]; 
		trees[0] = rootParse;
		trees[1] = rootDep;
		new DynamicTreeDemo(trees).show();
	}
	
	

	public static DefaultMutableTreeNode toDMTree(DefaultMutableTreeNode root, Tree tree){
		if(root == null){
			root = new DefaultMutableTreeNode();
		}
		
		String nodeContent = tree.nodeString();
		root.setUserObject(nodeContent);
		for(Tree c: tree.children()){
			DefaultMutableTreeNode n = toDMTree(null, c);
			root.add(n);
		}
		return root;
	}


	public static DefaultMutableTreeNode toDMTree(IndexedWord root, SemanticGraph dependencies){

		if(root == null){
			root = dependencies.getFirstRoot();
		}

		DefaultMutableTreeNode node = new DefaultMutableTreeNode();

		String nodeContent = root.value();

		for(SemanticGraphEdge edge: dependencies.edgeIterable()){
			if(edge.getDependent().equals(root)){
				nodeContent = "<-"+ edge.getRelation() +"- "+nodeContent;
				break;
			}
		}

		node.setUserObject(nodeContent);
		for(IndexedWord c: dependencies.getChildList(root)){
			DefaultMutableTreeNode n = toDMTree(c, dependencies);
			node.add(n);
		}
		return node;
	}


	public static Tree makeTreeRobust(Collection<TypedDependency> tdl){
		LinkedList<TypedDependency> toAssemble = new LinkedList<TypedDependency>();
		for(TypedDependency dep: tdl){
			toAssemble.add(dep);
		}
		Tree tree = makeTree(toAssemble, true, null);
		return tree;
	}
	/**
	 * 
	 * @param tdl
	 * @param fail set true to return a null tree if the constructiion fails
	 * @param tree null to build a new tree, or you can start with a partial tree
	 * @return
	 * Feb 22. a gov could be missing, which fails the tree construction process.
	 * Use GrammaticalStructure.root() instead, whose POS nodes to be removed.
	 */
	public static Tree makeTree(LinkedList<TypedDependency> toAssemble, boolean fail, Tree tree){
		if(tree == null){
			tree = lstf.newTreeNode("DUMMYROOT",null);
		}

		toAssemble.add(null);//
		int counter = toAssemble.size();

		while(toAssemble.size()>0){
			//1. pick the next dep
			TypedDependency dep = toAssemble.getFirst();
			if(dep == null){
				toAssemble.poll();
				if(counter-- >0){
					toAssemble.add(null);
					continue;
				}else{
					if(toAssemble.size()>0 && fail){
						tree = null;
					}
					break;
				}
			}

			//2. assemble it onto the tree
			Tree newRoot = putOnBranch(dep, tree);
			//2.1 success -> remove it from the set
			toAssemble.remove(dep);
			if(newRoot!=null){
				tree = newRoot;
				//				System.out.println(tree+" BetterText.makeTree()");
				//				System.out.println("Added:\t"+dep.gov() +"-->"+dep.dep());
			}else{
				//2.2 fail -> put it back at the tail of the set
				//				System.out.println("Skipd:\t"+dep.gov() +"-->"+dep.dep());
				//				System.out.print(".");
				toAssemble.add(toAssemble.size(), dep);
			}
		}
		return tree.getChild(0);
	}

	private static Tree putOnBranch(TypedDependency dep, Tree tree){
		/*
		 * Each node is a tree with a single child
		 */
		Tree mySubtree = lstf.newTreeNode(dep.gov().label(), new LinkedList<Tree>(dep.dep()));
		mySubtree.setValue("[<-"+ dep.reln() +"-] "+dep.dep().value());//nudge in the dependency relation information

		if(tree.children().length==0){
			if(tree.label().value().toString().equals("DUMMYROOT")){
				tree.addChild(mySubtree);
				return tree;
			}else{
				//Shouldn't happen
				System.err.println("Forgot to add a child earlier.");
				return null;
			}
		}else{
			//			System.err.println(dep.dep().label() +"\t[on]\t" + tree.label());
			for(Tree child:tree.children()){
				//if dep is child's parent, insert dep between child and its parent
				if( ((CoreLabel)child.label()).index() == dep.dep().label().index()){
					tree.removeChild(tree.objectIndexOf(child));
					mySubtree.addChild(child);
				}
			}
			if(mySubtree.children().length>1){
				tree.addChild(mySubtree);
				return tree;
			}
			
			for(Tree child:tree.children()){
				//if dep is Child's sibling, or child
				if( ((CoreLabel)child.label()).index() == dep.gov().label().index()){
					tree.addChild(mySubtree);
					return tree;
				}

				if(child.children().length>0){
					if(putOnBranch(dep, child)!=null){
						return tree;
					}
				}
			}
		}
		//			 tree.getLeaves() == null
		//check its childrens, recurisively.
		return null;
	}

		private static void showHelpMessage(){
			System.out.println("Usage:\tjava edu.nus.comp.nlp.stanford.UtilParser [-options] ");
			
			System.out.println("\t-t text\t \"The article to view can be provided as a single argument.\"");
			System.out.println("\t-example");
			System.out.println("\t-f file\t a plain text file to view");
		}	

}

class DynamicTreeDemo extends JPanel {

	private int newNodeSuffix = 1;
	JFrame frame = new JFrame("ParseTree");
	DynamicTree[] treePanels;
	int currentTreeId = -1;
	DynamicTree currentTree = null;
	

	public DynamicTreeDemo( DefaultMutableTreeNode[] node) {
		//create the components
		treePanels = new DynamicTree[node.length];
		for(int i = 0; i<node.length; i++){
			treePanels[i] = new DynamicTree(node[i]);
		    //populateTree(treePanel);
		}
		currentTreeId = 0;
		currentTree = treePanels[currentTreeId];
		wireTheTree();
	}
	
	private void wireTheTree(){ 
		JButton addButton = new JButton("Add");
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				treePanels[0].addObject("New Node " + newNodeSuffix++);
			}
		});

		JButton removeButton = new JButton("Remove");
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				treePanels[0].removeCurrentNode();
			}
		});

		JButton clearButton = new JButton("Clear");
		clearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				treePanels[0].clear();
			}
		});

		JButton expandButton = new JButton("Expand");
		expandButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//put your code here ... :p
				expandAll(treePanels[0].getTree(),true);
				expandAll(treePanels[1].getTree(),true);
			}
		});

		JButton collapseButton = new JButton("Collapse");
		collapseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//put your code here ... :p
				expandAll(treePanels[0].getTree(),false);
				expandAll(treePanels[1].getTree(),false);
			}
		});



		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
				System.exit(0);
			}
		});


		//Lay everything out.
		setLayout(new BorderLayout());
		treePanels[0].setPreferredSize(new Dimension(400, 150));
		treePanels[1].setPreferredSize(new Dimension(350, 150));
		add(treePanels[0], BorderLayout.CENTER);
		add(treePanels[1], BorderLayout.WEST);


		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(0, 1));
		//        panel.add(addButton);
		//        panel.add(removeButton);
		//        panel.add(clearButton);
		panel.add(expandButton);
		panel.add(collapseButton);
		panel.add(closeButton);
		add(panel, BorderLayout.EAST);
	}

	// If expand is true, expands all nodes in the tree.
	// Otherwise, collapses all nodes in the tree.
	public void expandAll(JTree tree, boolean expand) {
		TreeNode root = (TreeNode)tree.getModel().getRoot();

		// Traverse tree from root
		expandAll(tree, new TreePath(root), expand);
	}

	/**
	 * Not finished
	 * @param tree JTree
	 * @param expand boolean
	 * @param level int specify the number of level to expand
	 */
	public void expandAll(JTree tree, boolean expand, int level) {
		TreeNode root = (TreeNode)tree.getModel().getRoot();
		/** @todo to finish */
		// Traverse tree from root
		expandAll(tree, new TreePath(root), expand);
	}


	private void expandAll(JTree tree, TreePath parent, boolean expand) {
		// Traverse children
		TreeNode node = (TreeNode)parent.getLastPathComponent();
		if (node.getChildCount() >= 0) {
			for (Enumeration e=node.children(); e.hasMoreElements(); ) {
				TreeNode n = (TreeNode)e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				expandAll(tree, path, expand);
			}
		}

		// Expansion or collapse must be done bottom-up
		if (expand) {
			tree.expandPath(parent);
		} else {
			tree.collapsePath(parent);
		}
	}


	private void populateTree(DynamicTree treePanel) {
		String p1Name = new String("Parent 1");
		String p2Name = new String("Parent 2");
		String c1Name = new String("Child 1");
		String c2Name = new String("Child 2");

		DefaultMutableTreeNode p1, p2;

		p1 = treePanel.addObject(null, p1Name);
		p2 = treePanel.addObject(null, p2Name);

		treePanel.addObject(p1, c1Name);
		treePanel.addObject(p1, c2Name);

		treePanel.addObject(p2, c1Name);
		treePanel.addObject(p2, c2Name);
	}

	public void show(String title) {
		Container contentPane = frame.getContentPane();
		contentPane.setLayout(new GridLayout(1, 1));
		contentPane.add(this);
		frame.setTitle(title);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.pack();
		frame.setSize(new Dimension(800, 480));
		frame.setVisible(true);
	}

	public void show() {
		show(""); //unnamed
	}

}

class DynamicTree extends JPanel {
	protected DefaultMutableTreeNode rootNode;
	protected DefaultTreeModel treeModel;
	protected JTree tree;
	
	private Toolkit toolkit = Toolkit.getDefaultToolkit();

	public DynamicTree(DefaultMutableTreeNode node) {
		rootNode = node;
		//rootNode = new DefaultMutableTreeNode("Root Node");
		treeModel = new DefaultTreeModel(rootNode);
		treeModel.addTreeModelListener(new MyTreeModelListener());

		tree = new JTree(treeModel);
		tree.setEditable(true);
		tree.getSelectionModel().setSelectionMode
		(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setShowsRootHandles(true);

		JScrollPane scrollPane = new JScrollPane(tree);
		setLayout(new GridLayout(1,0));
		add(scrollPane);
	}

	/** Remove all nodes except the root node. */
	public void clear() {
		rootNode.removeAllChildren();
		treeModel.reload();
	}

	/** Remove the currently selected node. */
	public void removeCurrentNode() {
		TreePath currentSelection = tree.getSelectionPath();
		if (currentSelection != null) {
			DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)
					(currentSelection.getLastPathComponent());
			MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
			if (parent != null) {
				treeModel.removeNodeFromParent(currentNode);
				return;
			}
		}

		// Either there was no selection, or the root was selected.
		toolkit.beep();
	}

	/** Add child to the currently selected node. */
	public DefaultMutableTreeNode addObject(Object child) {
		DefaultMutableTreeNode parentNode = null;
		TreePath parentPath = tree.getSelectionPath();

		if (parentPath == null) {
			parentNode = rootNode;
		} else {
			parentNode = (DefaultMutableTreeNode)
					(parentPath.getLastPathComponent());
		}

		return addObject(parentNode, child, true);
	}

	public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent,
			Object child) {
		return addObject(parent, child, false);
	}

	public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent,
			Object child,
			boolean shouldBeVisible) {
		DefaultMutableTreeNode childNode =
				new DefaultMutableTreeNode(child);

		if (parent == null) {
			parent = rootNode;
		}

		treeModel.insertNodeInto(childNode, parent,
				parent.getChildCount());

		// Make sure the user can see the lovely new node.
		if (shouldBeVisible) {
			tree.scrollPathToVisible(new TreePath(childNode.getPath()));
		}
		return childNode;
	}

	public JTree getTree(){
		return tree;
	}

	class MyTreeModelListener implements TreeModelListener {
		public void treeNodesChanged(TreeModelEvent e) {
			DefaultMutableTreeNode node;
			node = (DefaultMutableTreeNode)
					(e.getTreePath().getLastPathComponent());

			/*
			 * If the event lists children, then the changed
			 * node is the child of the node we've already
			 * gotten.  Otherwise, the changed node and the
			 * specified node are the same.
			 */
			try {
				int index = e.getChildIndices()[0];
				node = (DefaultMutableTreeNode)
						(node.getChildAt(index));
			} catch (NullPointerException exc) {}

			System.out.println("The user has finished editing the node.");
			System.out.println("New value: " + node.getUserObject());
		}
		public void treeNodesInserted(TreeModelEvent e) {
		}
		public void treeNodesRemoved(TreeModelEvent e) {
		}
		public void treeStructureChanged(TreeModelEvent e) {
		}
	}
}

