function goTo(selectId) {
  const choice = document.getElementById(selectId).value;
  window.location.href = choice;
}

function playIntroVideo() {
  const video = document.getElementById('introVideo');
  const overlay = document.getElementById('videoOverlay');
  if (!video) return;

  if (overlay) overlay.classList.add('is-hidden');
  video.play();
}

document.addEventListener('DOMContentLoaded', () => {
  const video = document.getElementById('introVideo');
  const overlay = document.getElementById('videoOverlay');
  if (!video || !overlay) return;

  video.addEventListener('play', () => overlay.classList.add('is-hidden'));
  video.addEventListener('pause', () => {
    if (video.currentTime > 0 && !video.ended) overlay.classList.remove('is-hidden');
  });
  video.addEventListener('ended', () => overlay.classList.remove('is-hidden'));
});