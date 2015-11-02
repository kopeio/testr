package executor

import (
	"bytes"
	"fmt"
	"github.com/golang/glog"
	"github.com/golang/protobuf/proto"
	"github.com/kopeio/testr/go/pkg/model"
	"io"
	"net/http"
	"strings"
	"strconv"
)

type Client struct {
	baseUrl     string
	token       string
	executionKey *model.ExecutionKey
}

func NewClient(baseUrl string, executionKey *model.ExecutionKey, token string) *Client {
	if !strings.HasSuffix(baseUrl, "/") {
		baseUrl += "/"
	}
	c := &Client{
		baseUrl:     baseUrl,
		token:       token,
		executionKey:         executionKey,
	}
	return c
}


func (c *Client) buildUrl(service string, relativeUrl string) (string) {
	u := c.baseUrl + "api/" + service + "/" + c.executionKey.Job + "/" + c.executionKey.Revision + "/" +strconv.FormatInt(c.executionKey.Timestamp, 10)
	if relativeUrl != "" {
		u += "/" + relativeUrl
	}
	return u
}

func (c *Client) GetPlan() ([]byte, error) {
	// TODO: Use protobuf?
	u := c.buildUrl("system", "plan")
	request, err := http.NewRequest("GET", u, nil)
	if err != nil {
		return nil, fmt.Errorf("error building request (while fetching plan): %v", err)
	}

	body, err := c.doHttpRequest(request)
	return body, err
}

func (c *Client) UploadArtifact(artifact *Artifact) (int64, error) {
	u := c.buildUrl("blob", artifact.RelativePath)
	var body io.ReadCloser
	n, err := artifact.File.Seek(0, 2)
	if err != nil {
		return 0, fmt.Errorf("error seeking in artifact file (for upload): %v", err)
	}
	_, err = artifact.File.Seek(0, 0)
	if err != nil {
		return 0, fmt.Errorf("error seeking in artifact file (for upload): %v", err)
	}
	body = artifact.File
	request, err := http.NewRequest("PUT", u, body)
	if err != nil {
		return 0, fmt.Errorf("error building request (while uploading artifact): %v", err)
	}

	_, err = c.doHttpRequest(request)
	return n, err
}

func (c *Client) doHttpRequest(request *http.Request) ([]byte, error) {
	if c.token != "" {
		request.Header.Add("Authorization", c.token)
	}

	response, err := http.DefaultClient.Do(request)
	defer func() {
		if response != nil && response.Body != nil {
			err := response.Body.Close()
			if err != nil {
				glog.Warning("error closing http response body; %v", err)
			}
		}
	}()

	if err != nil {
		return nil, fmt.Errorf("error communicating with server (on %q): %v", request.URL, err)
	}
	if response.StatusCode != 200 {
		return nil, fmt.Errorf("server returned non-200 status code (on %q): %q", request.URL, response.Status)
	}

	var body []byte
	if response != nil && response.Body != nil {
		var buffer bytes.Buffer
		_, err := io.Copy(&buffer, response.Body)
		if err != nil {
			return nil, fmt.Errorf("error reading body (on %q): %v", request.URL, err)
		}
		body = buffer.Bytes()
	}
	return body, nil
}

func (c *Client) UploadResult(result *model.Execution) error {
	u := c.buildUrl("execution", "")

	resultBytes, err := proto.Marshal(result)
	if err != nil {
		return fmt.Errorf("error serializing execution data: %v", err)
	}
	body := bytes.NewReader(resultBytes)
	request, err := http.NewRequest("PUT", u, body)
	if err != nil {
		return fmt.Errorf("error building request (while uploading execution result): %v", err)
	}
	request.Header.Add("Content-Type", "application/x-protobuf")

	_, err = c.doHttpRequest(request)
	return err
}
