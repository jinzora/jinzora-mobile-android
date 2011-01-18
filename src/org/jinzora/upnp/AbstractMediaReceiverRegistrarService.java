package org.jinzora.upnp;

import java.beans.PropertyChangeSupport;

import org.teleal.cling.binding.annotations.UpnpAction;
import org.teleal.cling.binding.annotations.UpnpInputArgument;
import org.teleal.cling.binding.annotations.UpnpOutputArgument;
import org.teleal.cling.binding.annotations.UpnpService;
import org.teleal.cling.binding.annotations.UpnpServiceId;
import org.teleal.cling.binding.annotations.UpnpServiceType;
import org.teleal.cling.binding.annotations.UpnpStateVariable;
import org.teleal.cling.binding.annotations.UpnpStateVariables;
import org.teleal.cling.model.types.Base64Datatype;

@UpnpService(
        serviceId = @UpnpServiceId(namespace = "microsoft.com", value = "X_MS_MediaReceiverRegistrar"),
        serviceType = @UpnpServiceType(namespace = "microsoft.com", value = "X_MS_MediaReceiverRegistrar", version = 1)
)
@UpnpStateVariables({
        @UpnpStateVariable(
                name = "A_ARG_TYPE_DeviceID",
                sendEvents = false,
                datatype = "string"),
        @UpnpStateVariable(
                name = "A_ARG_TYPE_Result",
                sendEvents = false,
                datatype = "boolean"),
        @UpnpStateVariable(
                name = "A_ARG_TYPE_RegistrationReqMsg",
                sendEvents = false,
                datatype = "bin.base64"),
        @UpnpStateVariable(
                name = "A_ARG_TYPE_RegistrationRespMsg",
                sendEvents = false,
                datatype = "bin.base64"),
        @UpnpStateVariable(
                name = "AuthorizationGrantedUpdateID",
                sendEvents = false,
                datatype = "ui4",
                eventMinimumDelta = 1),
        @UpnpStateVariable(
                name = "AuthorizationDeniedUpdateID",
                sendEvents = false,
                datatype = "ui4",
                eventMinimumDelta = 1),
        @UpnpStateVariable(
                name = "ValidationSucceededUpdateID",
                sendEvents = false,
                datatype = "ui4"),
        @UpnpStateVariable(
                name = "ValidationRevokedUpdateID",
                sendEvents = false,
                datatype = "ui4")
})
public abstract class AbstractMediaReceiverRegistrarService {

    final protected PropertyChangeSupport propertyChangeSupport;

    protected AbstractMediaReceiverRegistrarService() {
        this(null);
    }

    protected AbstractMediaReceiverRegistrarService(PropertyChangeSupport propertyChangeSupport) {
        this.propertyChangeSupport = propertyChangeSupport != null ? propertyChangeSupport : new PropertyChangeSupport(this);
    }

    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    @UpnpAction(out = {
            @UpnpOutputArgument(name = "Result",
                    stateVariable = "A_ARG_TYPE_Result")
    })
    public boolean IsAuthorized(@UpnpInputArgument(name = "DeviceID", stateVariable = "A_ARG_TYPE_DeviceID") String deviceID) {
        return true;
    }

    @UpnpAction(out = {
            @UpnpOutputArgument(name = "Result",
                    stateVariable = "A_ARG_TYPE_Result")
    })
    public boolean IsValidated(@UpnpInputArgument(name = "DeviceID", stateVariable = "A_ARG_TYPE_DeviceID") String deviceID) {
        return true;
    }

    @UpnpAction(out = {
            @UpnpOutputArgument(name = "RegistrationRespMsg",
                    stateVariable = "A_ARG_TYPE_RegistrationRespMsg")
    })
    public Byte[] RegisterDevice(@UpnpInputArgument(name = "RegistrationReqMsg", stateVariable = "A_ARG_TYPE_RegistrationReqMsg") Byte[] registrationReqMsg) {
        return new Byte[]{};
    }
}