/*
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.model.scripts;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.meveo.model.BusinessEntity;
import org.meveo.model.ExportIdentifier;
import org.meveo.model.security.Role;

@Entity
@ExportIdentifier({ "code" })
@Table(name = "MEVEO_SCRIPT_INSTANCE", uniqueConstraints = @UniqueConstraint(columnNames = { "CODE","PROVIDER_ID" }))
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "MEVEO_SCRIPT_INSTANCE_SEQ")
@NamedQueries({			
@NamedQuery(name = "ScriptInstance.countScriptInstanceOnError", 
	           query = "select count (*) from ScriptInstance o where o.error=:isError and o.provider=:provider"),
	           
@NamedQuery(name = "ScriptInstance.getScriptInstanceOnError", 
	           query = "from ScriptInstance o where o.error=:isError and o.provider=:provider")
	})
public class ScriptInstance extends BusinessEntity  {

	private static final long serialVersionUID = -5517252645289726288L;

	@Column(name = "SCRIPT", nullable = false, columnDefinition="TEXT")
	private String script;

	@Enumerated(EnumType.STRING)
	@Column(name = "SRC_TYPE")
	ScriptTypeEnum scriptTypeEnum = ScriptTypeEnum.JAVA;
	
	@OneToMany(mappedBy = "scriptInstance", orphanRemoval=true,fetch = FetchType.EAGER)
	private List<ScriptInstanceError> scriptInstanceErrors = new ArrayList<ScriptInstanceError>();

	
	@Column(name = "IS_ERROR")
	private Boolean error = false;
	
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "ADM_SCRIPT_EXEC_ROLE", joinColumns = @JoinColumn(name = "SCRIPT_INSTANCE_ID"), inverseJoinColumns = @JoinColumn(name = "ROLE_ID"))
    private List<Role> executionRoles = new ArrayList<Role>();
    
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "ADM_SCRIPT_SOURC_ROLE", joinColumns = @JoinColumn(name = "SCRIPT_INSTANCE_ID"), inverseJoinColumns = @JoinColumn(name = "ROLE_ID"))
    private List<Role> sourcingRoles = new ArrayList<Role>();
	
	public ScriptInstance(){

	}

	/**
	 * @return the scriptTypeEnum
	 */
	public ScriptTypeEnum getScriptTypeEnum() {
		return scriptTypeEnum;
	}


	/**
	 * @param scriptTypeEnum the scriptTypeEnum to set
	 */
	public void setScriptTypeEnum(ScriptTypeEnum scriptTypeEnum) {
		this.scriptTypeEnum = scriptTypeEnum;
	}



	/**
	 * @return the script
	 */
	public String getScript() {
		return script;
	}



	/**
	 * @param script the script to set
	 */
	public void setScript(String script) {
		this.script = script;
	}

	/**
	 * @return the scriptInstanceErrors
	 */
	public List<ScriptInstanceError> getScriptInstanceErrors() {
		return scriptInstanceErrors;
	}

	/**
	 * @param scriptInstanceErrors the scriptInstanceErrors to set
	 */
	public void setScriptInstanceErrors(List<ScriptInstanceError> scriptInstanceErrors) {
		this.scriptInstanceErrors = scriptInstanceErrors;
	}

	/**
	 * @return the error
	 */
	public Boolean getError() {
		return error;
	}

	/**
	 * @param error the error to set
	 */
	public void setError(Boolean error) {
		this.error = error;
	}

	/**
	 * @return the executionRoles
	 */
	public List<Role> getExecutionRoles() {
		return executionRoles;
	}

	/**
	 * @param executionRoles the executionRoles to set
	 */
	public void setExecutionRoles(List<Role> executionRoles) {
		this.executionRoles = executionRoles;
	}

	/**
	 * @return the sourcingRoles
	 */
	public List<Role> getSourcingRoles() {
		return sourcingRoles;
	}

	/**
	 * @param sourcingRoles the sourcingRoles to set
	 */
	public void setSourcingRoles(List<Role> sourcingRoles) {
		this.sourcingRoles = sourcingRoles;
	}




}