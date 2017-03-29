export function checkHttpResponseStatus(response) {
  if (response.status >= 200 && response.status < 300) {
    return response;
  } else {
    var error = new Error(response.status);
    error.response = response;
    throw error;
  };
};

export function createJsonPostRequest(body) {
  return {
    method : 'POST',
    headers :
        {'Accept' : 'application/json', 'Content-Type' : 'application/json'},
    body : JSON.stringify(body)
  };
};

export function sendGetStateRequest() {
  return fetch("http://localhost:8080/_/state")
  .then(checkHttpResponseStatus)
  .then(response => response.json());
}