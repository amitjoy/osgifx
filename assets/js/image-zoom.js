document.addEventListener('DOMContentLoaded', () => {
    const images = document.querySelectorAll('section img:not(.no-zoom)');
    
    if (images.length === 0) return;

    // Create the overlay elements
    const overlay = document.createElement('div');
    overlay.className = 'image-zoom-overlay';
    
    const zoomImg = document.createElement('img');
    zoomImg.alt = 'Zoomed view';
    
    overlay.appendChild(zoomImg);
    document.body.appendChild(overlay);

    const openZoom = (img) => {
        zoomImg.src = img.src;
        overlay.style.display = 'flex';
        // Force reflow for opacity transition
        overlay.offsetHeight;
        overlay.classList.add('active');
        document.body.style.overflow = 'hidden';
    };

    const closeZoom = () => {
        overlay.classList.remove('active');
        document.body.style.overflow = '';
        // Wait for transition before hiding
        setTimeout(() => {
            if (!overlay.classList.contains('active')) {
                overlay.style.display = 'none';
            }
        }, 300);
    };

    images.forEach(img => {
        img.addEventListener('click', (e) => {
            e.stopPropagation();
            openZoom(img);
        });
    });

    overlay.addEventListener('click', closeZoom);

    // Escape listener
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && overlay.classList.contains('active')) {
            closeZoom();
        }
    });

    // Touch support for closing
    let touchStartY = 0;
    overlay.addEventListener('touchstart', (e) => {
        touchStartY = e.touches[0].clientY;
    }, { passive: true });

    overlay.addEventListener('touchend', (e) => {
        const touchEndY = e.changedTouches[0].clientY;
        if (Math.abs(touchEndY - touchStartY) > 50) {
            closeZoom();
        }
    }, { passive: true });
});
