// Production build. apiBaseUrl must point at the deployed backend's origin.
// On Render this is https://<backend-service-name>.onrender.com — keep it in
// sync with the actual backend URL (see render.yaml service "payhub-api").
export const environment = {
  production: true,
  apiBaseUrl: 'https://payhub-api.onrender.com',
};
