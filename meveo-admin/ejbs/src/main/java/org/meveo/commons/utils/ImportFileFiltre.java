/*
* (C) Copyright 2009-2013 Manaty SARL (http://manaty.net/) and contributors.
*
* Licensed under the GNU Public Licence, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.gnu.org/licenses/gpl-2.0.txt
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.meveo.commons.utils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author anasseh
 *
 */
public class ImportFileFiltre implements  FilenameFilter{
	   private String  prefix = null;
	    private String  ext = null;
	   
	    
		public ImportFileFiltre(String prefix,String ext){
			this.prefix = prefix;
			this.ext    = ext;
			if(StringUtils.isBlank(prefix)){
				this.prefix = "*";
			}
			if(StringUtils.isBlank(ext)){
				this.ext = "*";
			}
		}
		public boolean accept(File dir,String name) {
		   if(name == null) return false;
		if( ("*".equals(ext)    ||  name.toUpperCase().endsWith("."+ext.toUpperCase())) && 
			("*".equals(prefix) ||	name.startsWith(prefix))
			){
			return true;
		}
		else {
			return false;
		}
	}
		
	}