package com.austinv11.kotbot.github.events;

public class Commit {
	
	/**
	 * The branch info in the form "refs/heads/{BRANCH}"
	 */
	public String ref;
	
	/**
	 * The commits pushed.
	 */
	public CommitContent[] commits;
	
	/**
	 * The repository the commits were pushed to.
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
	
	public class CommitContent {
		
		/**
		 * The unique id for the commit.
		 */
		public String id;
		
		/**
		 * The url for the commit.
		 */
		public String url;
		
		/**
		 * Commit message.
		 */
		public String message;
		
		/**
		 * The author of the commit.
		 */
		public AuthorContent author;
		
		public class AuthorContent {
			
			/**
			 * The login name.
			 */
			public String username;
		}
	}
}
