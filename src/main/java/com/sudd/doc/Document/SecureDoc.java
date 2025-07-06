package com.sudd.doc.Document;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

import com.sudd.doc.Document.Domain.RequestContext;
import com.sudd.doc.Document.Enum.Authority;
import com.sudd.doc.Document.entity.RolesEntity;
import com.sudd.doc.Document.repository.RoleRepository;

@SpringBootApplication
@EnableJpaAuditing // track who created or modified an entity and when.
@EnableAsync // is used to allow asynchronous method execution
public class SecureDoc {

	public static void main(String[] args) {
		SpringApplication.run(SecureDoc.class, args);
	}
     // Dummy data 
	// @Bean
	CommandLineRunner CommandLineRunner(RoleRepository repository) {
		return args -> {
			// we can add any number here as long as its long , 
			// currently there is no relation so we need to have constraint
			RequestContext.setUserId(0L); 
			var userRole = new RolesEntity();
			userRole.setAuthority(Authority.USER);
			userRole.setName(Authority.USER.toString());
			repository.save(userRole);

			var apiAdminRole = new RolesEntity();
			apiAdminRole.setAuthority(Authority.ADMIN);
			apiAdminRole.setName(Authority.ADMIN.toString());
			repository.save(apiAdminRole);
			RequestContext.start();

		};
	}

}
