package io.kope.testr.endpoints;

import io.kope.testr.auth.Authenticator;
import io.kope.testr.auth.Authenticator.Authentication;
import io.kope.testr.auth.SignedTokenAuthenticator.SignedTokenAuthentication;
import io.kope.testr.protobuf.auth.Auth.AuthPermission;
import io.kope.testr.protobuf.model.Model.ExecutionKey;
import io.kope.testr.protobuf.model.Model.JobData;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Strings;

public class EndpointBase {

    @Inject
    HttpServletRequest request;

    @Inject
    Authenticator authenticator;

    private Authentication authentication;

    protected ExecutionKey buildExecutionKey(String job, String revision, long timestamp) {
        // TODO: Basic validation?
        ExecutionKey.Builder b = ExecutionKey.newBuilder();
        b.setJob(job);
        b.setRevision(revision);
        b.setTimestamp(timestamp);
        return b.build();
    }

    protected void requireExecutorToken(ExecutionKey executionKey) {
        Authentication authentication = this.authenticate();
        if (authentication == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        if (!authentication.isExecutorToken(executionKey)) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
    }

    public String getExecutorToken() {
        Authentication authentication = authenticate();
        if (authentication == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        if (authentication instanceof SignedTokenAuthentication) {
            return ((SignedTokenAuthentication) authentication).getToken();
        }

        throw new IllegalStateException("unexpected authentication type");
    }

    // TODO: Move to filter
    Authentication authenticate() {
        if (this.authentication != null) {
            return this.authentication;
        }
        String authString = request.getHeader("Authorization");
        if (Strings.isNullOrEmpty(authString)) {
            return null;
        }

        Authentication authentication = authenticator.authenticate(authString);
        if (authentication != null) {
            this.authentication = authentication;
        }
        return authentication;
    }

    // func (r *Request) methodNotAllowed() {
    // glog.V(2).Infof("Bad method for request: %s %s", r.r.Method, r.r.URL)
    //
    // http.Error(r.w, "method not allowed", http.StatusMethodNotAllowed)
    // }
    //
    // func (r *Request) internalServerError(err error) {
    // glog.Warningf("Internal server error processing request: %v", err)
    // http.Error(r.w, "internal server error", http.StatusInternalServerError)
    // }
    //
    // func (r *Request) badRequest() {
    // http.Error(r.w, "bad request", http.StatusBadRequest)
    // }
    //
    // func (r *Request) needAuthentication() {
    // // TODO: Send correct headers
    // http.Error(r.w, "unauthorized", http.StatusUnauthorized)
    // }
    //
    // func (r *Request) forbidden() {
    // http.Error(r.w, "forbidden", http.StatusForbidden)
    // }
    //
    // func (r *Request) notFound() {
    // http.NotFound(r.w, r.r)
    // }
    //
    // func (r *Request) sendJsonResponse(data interface{}) {
    // jsonBytes, err := json.Marshal(data)
    // if err != nil {
    // r.internalServerError(fmt.Errorf("error marshalling json data: %v", err))
    // return
    // }
    //
    // r.w.Header().Set("Content-Type", "application/json")
    // _, err = r.w.Write(jsonBytes)
    // if err != nil {
    // glog.Warning("error writing http response: %v", err)
    // }
    // }
    //
    // func (r *Request) sendStringResponse(contentType string, data string) {
    // r.w.Header().Set("Content-Type", contentType)
    // _, err := r.w.Write([]byte(data))
    // if err != nil {
    // glog.Warning("error writing http response: %v", err)
    // }
    // }
    //
    // func (r *Request) sendOK() {
    // r.w.WriteHeader(200)
    // }
    //
    // func (r *Request) sendProtoResponse(data proto.Message) {
    // marshaler := jsonpb.Marshaler{}
    // marshaler.Indent = "  "
    // jsonString, err := marshaler.MarshalToString(data)
    // if err != nil {
    // r.internalServerError(fmt.Errorf("error marshalling json data: %v", err))
    // return
    // }
    //
    // r.w.Header().Set("Content-Type", "application/json")
    // _, err = r.w.Write([]byte(jsonString))
    // if err != nil {
    // glog.Warning("error writing http response: %v", err)
    // }
    // }
    //
    // func (r *Request) sendBlobResponse(data Blob) {
    // // r.w.Header().Set("Content-Type", "application/json")
    // r.w.Header().Set("Content-Type", "application/octet-stream")
    // r.w.Header().Set("Content-Length", strconv.FormatInt(data.Length(), 10))
    //
    // // TODO: gzip
    // err := data.WriteTo(r.w)
    // if err != nil {
    // glog.Warning("error writing blob to http response: %v", err)
    // }
    // }
    //
    // // Reads the posted body, in JSON format
    // func (r *Request) ReadProtoBody(dest proto.Message) error {
    // bodyBytes, err := ioutil.ReadAll(r.r.Body)
    // if err != nil {
    // return fmt.Errorf("error reading body: %v", err)
    // }
    //
    // err = proto.Unmarshal(bodyBytes, dest)
    // if err != nil {
    // return fmt.Errorf("error parsing body: %v", err)
    // }
    //
    // return nil
    // }

    protected void requireAuthorized(JobData job, AuthPermission level) {

        if (job.getIsPublic() && level == AuthPermission.READ) {
            return;
        }

        Authentication authentication = authenticate();
        if (authentication == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        if (authentication.isAuthorized(level, job)) {
            return;
        }

        throw new WebApplicationException(Status.FORBIDDEN);
    }

    // func (r*Request) RequireBody(data proto.Message) bool {
    // err := r.ReadProtoBody(data)
    // if err != nil {
    // r.badRequest()
    // return false
    // }
    // return true
    // }

}
