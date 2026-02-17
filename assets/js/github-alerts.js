document.addEventListener('DOMContentLoaded', function() {
  const blockquotes = document.querySelectorAll('blockquote');
  
  const alerts = {
    'NOTE': {
      class: 'markdown-alert-note',
      title: 'Note',
      icon: '<svg class="octicon octicon-info mr-2" viewBox="0 0 16 16" version="1.1" width="16" height="16" aria-hidden="true"><path d="M0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8Zm8-6.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13ZM6.5 7.75A.75.75 0 0 1 7.25 7h1.5a.75.75 0 0 1 .75.75v2.75h.25a.75.75 0 0 1 0 1.5h-2a.75.75 0 0 1 0-1.5h.25v-2h-.25a.75.75 0 0 1-.75-.75ZM8 6a1 1 0 1 1 0-2 1 1 0 0 1 0 2Z"></path></svg>'
    },
    'TIP': {
      class: 'markdown-alert-tip',
      title: 'Tip',
      icon: '<svg class="octicon octicon-light-bulb mr-2" viewBox="0 0 16 16" version="1.1" width="16" height="16" aria-hidden="true"><path d="M8 1.5c-2.363 0-4 1.69-4 3.75 0 .984.424 1.625.984 2.304l.214.253c.223.264.47.556.673.848.284.411.537.896.621 1.49a.75.75 0 0 1-1.484.214c-.04-.282-.149-.559-.316-.801-.322-.464-.676-.89-1.04-1.326l-.273-.321c-.752-.916-1.379-1.89-1.379-3.261 0-3.37 2.686-5.25 6-5.25C11.314 1.5 14 3.38 14 5.25c0 1.371-.627 2.345-1.379 3.26l-.273.321c-.364.436-.718.863-1.04 1.327-.167.242-.276.519-.316.8.006.002.013.004.019.006a.75.75 0 0 1-.39 1.448c-.06-.02-.12-.04-.18-.06.084-.594.337-1.079.621-1.489.203-.292.45-.584.673-.848l.214-.253c.56-.679.984-1.32.984-2.304 0-2.06-1.637-3.75-4-3.75ZM5.5 12.5a.75.75 0 0 1 .75-.75h3.5a.75.75 0 0 1 0 1.5h-3.5a.75.75 0 0 1-.75-.75ZM6.25 14a.75.75 0 0 0 0 1.5h3.5a.75.75 0 0 0 0-1.5h-3.5Z"></path></svg>'
    },
    'IMPORTANT': {
      class: 'markdown-alert-important',
      title: 'Important',
      icon: '<svg class="octicon octicon-report mr-2" viewBox="0 0 16 16" version="1.1" width="16" height="16" aria-hidden="true"><path d="M0 1.75C0 .784.784 0 1.75 0h12.5C15.216 0 16 .784 16 1.75v9.5A1.75 1.75 0 0 1 14.25 13H8.06l-2.573 2.573A1.458 1.458 0 0 1 3 14.543V13H1.75A1.75 1.75 0 0 1 0 11.25v-9.5Zm1.75-.25a.25.25 0 0 0-.25.25v9.5c0 .138.112.25.25.25h2a.75.75 0 0 1 .75.75v2.19l2.72-2.72a.75.75 0 0 1 .53-.22h6.5a.25.25 0 0 0 .25-.25v-9.5a.25.25 0 0 0-.25-.25H1.75ZM8 9a1 1 0 1 1 0-2 1 1 0 0 1 0 2ZM8 4a.75.75 0 0 1 .75.75v2.5a.75.75 0 0 1-1.5 0v-2.5A.75.75 0 0 1 8 4Z"></path></svg>'
    },
    'WARNING': {
      class: 'markdown-alert-warning',
      title: 'Warning',
      icon: '<svg class="octicon octicon-alert mr-2" viewBox="0 0 16 16" version="1.1" width="16" height="16" aria-hidden="true"><path d="M6.457 1.047c.659-1.234 2.427-1.234 3.086 0l6.082 11.378A1.75 1.75 0 0 1 14.082 15H1.918a1.75 1.75 0 0 1-1.543-2.575Zm1.763.707a.25.25 0 0 0-.44 0L1.698 13.132a.25.25 0 0 0 .22.368h12.164a.25.25 0 0 0 .22-.368Zm.53 3.996v2.5a.75.75 0 0 1-1.5 0v-2.5a.75.75 0 0 1 1.5 0ZM9 11a1 1 0 1 1-2 0 1 1 0 0 1 2 0Z"></path></svg>'
    },
    'CAUTION': {
      class: 'markdown-alert-caution',
      title: 'Caution',
      icon: '<svg class="octicon octicon-stop mr-2" viewBox="0 0 16 16" version="1.1" width="16" height="16" aria-hidden="true"><path d="M4.47.22A.749.749 0 0 1 5 0h6c.199 0 .389.079.53.22l4.25 4.25c.141.14.22.331.22.53v6a.749.749 0 0 1-.22.53l-4.25 4.25A.749.749 0 0 1 11 16H5a.749.749 0 0 1-.53-.22L.22 11.53A.749.749 0 0 1 0 11V5c0-.199.079-.389.22-.53Zm.84 1.28L1.5 5.31v5.38l3.81 3.81h5.38l3.81-3.81V5.31L10.69 1.5ZM8 4a.75.75 0 0 1 .75.75v3.5a.75.75 0 0 1-1.5 0v-3.5A.75.75 0 0 1 8 4Zm0 8a1 1 0 1 1 0-2 1 1 0 0 1 0 2Z"></path></svg>'
    }
  };

  blockquotes.forEach(blockquote => {
    const firstP = blockquote.querySelector('p');
    if (!firstP) return;

    const content = firstP.innerHTML.trim();
    const match = content.match(/^\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]/);

    if (match) {
      const type = match[1];
      const alert = alerts[type];
      
      // Remove the [!TYPE] text (and optional following newline/br)
      firstP.innerHTML = content.replace(/^\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]\s*(<br>)?\s*/, '');
      
      // Add class to blockquote
      blockquote.classList.add('markdown-alert', alert.class);
      
      // Create title element
      const titleDiv = document.createElement('div');
      titleDiv.className = 'markdown-alert-title';
      titleDiv.innerHTML = `${alert.icon} ${alert.title}`;
      
      // Insert title before the first p
      blockquote.insertBefore(titleDiv, firstP);
    }
  });
});
