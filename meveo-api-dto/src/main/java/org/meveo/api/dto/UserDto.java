package org.meveo.api.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.model.admin.User;
import org.meveo.model.security.Role;

/**
 * @author Mohamed Hamidi
 * @since Mai 23, 2016
 **/
@XmlRootElement(name = "User")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserDto extends BaseDto {

	private static final long serialVersionUID = -6633504145323452803L;

	@XmlElement(required = true)
	private String username;

	@XmlElement(required = true)
	private String password;

	@XmlElement(required = true)
	private String email;

	@XmlElement()
	private String provider;

	private String firstName;
	private String lastName;

	@XmlElementWrapper
    @XmlElement(name="role")
	private List<String> roles;
	
	@Deprecated//use roles field
	private String role;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public UserDto() {}
	


	public UserDto(User user) {
		if(user.getName()!=null){
		firstName = user.getName().getFirstName();
		lastName = user.getName().getLastName();	
		}
		username = user.getUserName();
		provider = user.getProvider().getCode();
		email=user.getEmail();

		if (user.getRoles() != null) {
			roles = new ArrayList<String>();
			for (Role r : user.getRoles()) {
				roles.add(r.getName());
				role=r.getName();
			}
		}
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
	
	

	/**
	 * @return the role
	 */
	public String getRole() {
		return role;
	}

	/**
	 * @param role the role to set
	 */
	public void setRole(String role) {
		this.role = role;
	}

	@Override
	public String toString() {
		return "UserDto [username=" + username + ", password=" + password + ", email=" + email + ", provider=" + provider + ", firstName=" + firstName + ", lastName=" + lastName + ", roles=" + roles + ", role=" + role + "]";
	}



}
