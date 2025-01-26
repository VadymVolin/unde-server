function exit() {
    fetch(window.location.href + 'unde/exit', {
      method: "POST", // Make sure the method matches the plugin configuration
      headers: {
        "Content-Type": "application/json",
      },
    })
      .then((response) => {
        if (response.ok) {
          console.log("Shutdown initiated.");
        } else {
          console.error("Shutdown failed: ", response.status);
        }
      })
      .catch((error) => {
        console.error("Error: ", error);
      });
}
