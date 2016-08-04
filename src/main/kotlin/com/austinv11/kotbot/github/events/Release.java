package com.austinv11.kotbot.github.events;

public class Release {
	
	/**
	 * The action that was performed. Currently, can only be "published".
	 */
	public String action;
	
	/**
	 * The actual release object.
	 */
	public ReleaseContent release;
	
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
	
	public class ReleaseContent {
		
		/**
		 * The link to the release.
		 */
		public String html_url;
		
		/**
		 * The tag name.
		 */
		public String tag_name;
		
		/**
		 * The release name.
		 */
		public String name;
		
		/**
		 * The release description.
		 */
		public String body;
		
		/**
		 * Whether this is a draft.
		 */
		public boolean draft;
		
		/**
		 * Whether this is a pre-release.
		 */
		public boolean prerelease;
		
		/**
		 * The author of the release.
		 */
		public AuthorContent author;
		
		public class AuthorContent {
			
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
