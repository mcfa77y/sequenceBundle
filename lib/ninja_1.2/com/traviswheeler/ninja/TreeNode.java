package com.traviswheeler.ninja;

public class TreeNode {
	
	//public TreeNode parent = null;
	public TreeNode leftChild = null;
	public TreeNode rightChild = null;
	public String name;
	public float length = Float.MAX_VALUE; 
	
	public TreeNode() {
		name = "";
	}

	public TreeNode(String name) {
		this.name = name;
	}

	
	final public void buildTreeString (StringBuffer sb) {
		String len;
		if (length == Float.MAX_VALUE)
			len = "";
		else 
			len = ":" + String.format("%.5f",length);
	
		if (null == leftChild) {
			sb.append(name  + len);
		} else {
			sb.append("(");
			leftChild.buildTreeString(sb);
			sb.append(",");
			rightChild.buildTreeString(sb);
			sb.append(")" + len);
		} 

		
	}	
	
}
