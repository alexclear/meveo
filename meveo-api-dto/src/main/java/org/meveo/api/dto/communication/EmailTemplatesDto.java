package org.meveo.api.dto.communication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author Tyshan　Shi(tyshan@manaty.net)
 * @date Jun 3, 2016 4:59:32 AM
 *
 */
@XmlType(name = "EmailTemplates")
@XmlAccessorType(XmlAccessType.FIELD)
public class EmailTemplatesDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7291683806771214766L;
	
	private List<EmailTemplateDto> emailTemplates;

	public List<EmailTemplateDto> getEmailTemplates() {
		if(emailTemplates==null){
			emailTemplates=new ArrayList<EmailTemplateDto>();
		}
		return emailTemplates;
	}

	public void setEmailTemplates(List<EmailTemplateDto> emailTemplates) {
		this.emailTemplates = emailTemplates;
	}
	

}

