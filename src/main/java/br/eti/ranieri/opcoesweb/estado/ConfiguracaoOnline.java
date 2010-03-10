/*
 *  Copyright 2009 ranieri.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package br.eti.ranieri.opcoesweb.estado;

import org.apache.wicket.IClusterable;

/**
 *
 * @author ranieri
 */
public class ConfiguracaoOnline implements IClusterable {
	private boolean usarProxy;
    private String proxyURL;
    private String jsessionid;

    public boolean isConfigurado() {
        if (usarProxy) {
            if (proxyURL == null || proxyURL.length() == 0) {
                return false;
            }
        }
        return jsessionid != null && jsessionid.length() > 0;
    }

    public String getJsessionid() {
        return jsessionid;
    }

    public void setJsessionid(String jsessionid) {
        this.jsessionid = jsessionid;
    }

    public String getProxyURL() {
        return proxyURL;
    }

    public void setProxyURL(String proxyURL) {
        this.proxyURL = proxyURL;
    }

    public boolean isUsarProxy() {
        return usarProxy;
    }

    public void setUsarProxy(boolean usarProxy) {
        this.usarProxy = usarProxy;
    }
}
