package com.austinv11.kotbot.github.events;

public class PullRequest {
	
	/**
	 * The action that was performed. Can be one of "assigned", "unassigned", "labeled", "unlabeled", "opened", 
	 * "edited", "closed", or "reopened", or "synchronize". If the action is "closed" and the merged key is false, 
	 * the pull request was closed with unmerged commits. If the action is "closed" and the merged key is true, the 
	 * pull request was merged.
	 */
	public String action;
	
	/**
	 * The pull request number.
	 */
	public int number;
	
	/**
	 * The pull request object.
	 */
	public PullRequestContent pull_request;
	
	/**
	 * The repository the pull request was submitted to.
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
	
	public class PullRequestContent {
		
		/**
		 * The issue number.
		 */
		public int number;
		
		/**
		 * The issue url.
		 */
		public String html_url;
		
		/**
		 * Whether it was merged.
		 */
		public boolean merged;
		
		/**
		 * The issue title.
		 */
		public String title;
		
		/**
		 * The issue body.
		 */
		public String body;
		
		/**
		 * Additions to the repo.
		 */
		public int additions;
		
		/**
		 * Deletions from the repo.
		 */
		public int deletions;
		
		/**
		 * The user who created this pull request.
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
