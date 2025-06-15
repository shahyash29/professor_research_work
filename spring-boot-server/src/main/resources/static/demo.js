/**
 * demo.js
 * Assumes test-credentials.js defines window.HERE_API_KEY
 */
(function() {
  // 1️⃣ Initialize the HERE platform
  const platform = new H.service.Platform({
    apikey: window.HERE_API_KEY
  });
  const defaultLayers = platform.createDefaultLayers();

  // 2️⃣ Create the map
  const map = new H.Map(
    document.getElementById('map'),
    defaultLayers.vector.normal.map,
    {
      center: { lat: 34.019929, lng: -118.503893 },
      zoom: 13,
      pixelRatio: window.devicePixelRatio || 1
    }
  );
  window.addEventListener('resize', () => map.getViewPort().resize());

  // enable interactions + UI
  new H.mapevents.Behavior(new H.mapevents.MapEvents(map));
  H.ui.UI.createDefault(map, defaultLayers);

  // 3️⃣ Draw initial circle and fetch data
  addCircleToMap(map);
  fetchAndVisualizeTrafficData(map);

  /**
   * Adds a circular overlay for the area of interest.
   */
  function addCircleToMap(map) {
    map.addObject(new H.map.Circle(
      { lat: 34.019929, lng: -118.503893 },
      50000,
      {
        style: {
          strokeColor: 'rgba(55, 85, 170, 0.6)',
          lineWidth: 2,
          fillColor: 'rgba(0, 128, 0, 0.7)'
        }
      }
    ));
  }

  /**
   * Fetches traffic data from your Spring Boot API and visualizes it.
   */
  async function fetchAndVisualizeTrafficData(map) {
    try {
      const res = await fetch('/api/traffic');
      if (!res.ok) {
        console.error(`Error fetching traffic data: ${res.statusText}`);
        return;
      }
      const data = await res.json();
      visualizeTrafficData(map, data.flow || []);
    } catch (err) {
      console.error('Error fetching traffic data:', err);
    }
  }

  /**
   * Renders traffic flow polylines on the map.
   */
  function visualizeTrafficData(map, flowArray) {
    // clear previous objects (including the circle), then re-add circle
    map.removeObjects(map.getObjects());
    addCircleToMap(map);

    if (!Array.isArray(flowArray) || flowArray.length === 0) {
      console.warn('No traffic flow data available to render.');
      return;
    }

    flowArray.forEach(segment => {
      const geometry = segment.currentFlow?.geometry || [];
      if (geometry.length) {
        const lineString = new H.geo.LineString();
        geometry.forEach(pt => {
          const [lat, lng] = pt.split(',').map(Number);
          if (!isNaN(lat) && !isNaN(lng)) {
            lineString.pushPoint({ lat, lng });
          }
        });
        map.addObject(new H.map.Polyline(lineString, {
          style: {
            lineWidth: 6,
            strokeColor: 'rgba(255, 0, 0, 0.7)'
          }
        }));
      }
    });
  }
})();
