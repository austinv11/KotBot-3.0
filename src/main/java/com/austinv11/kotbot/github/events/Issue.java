package com.austinv11.kotbot.github.events;

public class Issue {
	
	/**
	 * The action that was performed. Can be one of "assigned", "unassigned", "labeled", "unlabeled", "opened", 
	 * "edited", "closed", or "reopened".
	 */
	public String action;
	
	/**
	 * The actual issue.
	 */
	public IssueContent issue;
	
	/**
	 * The repository the issues were submitted to.
	 */
	public RepositoryContent repository;
	
	public class RepositoryContent {
		
		/**
		 * The repo's name.
		 */
		public String full_name;
		
		/**
		 * The repo's url.
		 */
		public String url;
	}
	
	public class IssueContent {
		
		/**
		 * The issue number.
		 */
		public int number;
		
		/**
		 * The issue url.
		 */
		public String html_url;
		
		/**
		 * The state (open or closed).
		 */
		public String state;
		
		/**
		 * The issue title.
		 */
		public String title;
		
		/**
		 * The issue body.
		 */
		public String body;
		
		/**
		 * The user who created this issue.
		 */
		public UserContent user;
		
		public class UserContent {
			
			/**
			 * The login name.
			 */
			public String login;
			
			/**
			 * The link to the user.
			 */
			public String html_url;
		}
	}
}
