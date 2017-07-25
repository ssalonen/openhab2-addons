/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.helios.internal;

/**
 * This class represents an exception thrown by the {@link HeliosCommunicator} class.
 *
 * @author Bernhard Bauer - Initial contribution
 */
public class HeliosException extends Exception {
    public HeliosException (String msg) {
        super(msg);
    }
}
