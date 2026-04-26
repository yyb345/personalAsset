<template>
  <div class="landing-page">
    <!-- Navigation -->
    <nav class="navbar">
      <div class="nav-container">
        <div class="nav-logo">
          <div class="logo-icon">
            <svg viewBox="0 0 90 20" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="90" height="20" rx="2" fill="#FF0000"/>
              <text x="45" y="14" font-family="Arial, sans-serif" font-size="12" font-weight="bold" fill="white" text-anchor="middle">EE</text>
            </svg>
          </div>
          <span class="logo-text">X Learning</span>
        </div>
        <ul class="nav-menu" :class="{ active: mobileMenuOpen }">
          <li><a href="#features" @click="scrollTo('features')">Features</a></li>
          <li><a href="#testimonials" @click="scrollTo('testimonials')">Testimonials</a></li>
          <li><a href="#pricing" @click="scrollTo('pricing')">Pricing</a></li>
        </ul>
        <div class="nav-buttons">
          <router-link to="/login" class="btn-secondary">Sign In</router-link>
          <router-link to="/dashboard/youtube" class="btn-primary">Get Started</router-link>
        </div>
        <button class="mobile-menu-btn" @click="mobileMenuOpen = !mobileMenuOpen">☰</button>
      </div>
    </nav>

    <!-- Hero Section -->
    <section class="hero">
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="gradient-text">Master English</span>
          <br>Through Video Learning
        </h1>
        <p class="hero-subtitle">
          AI-powered English learning platform for all levels.
          <br>Learn from YouTube videos, practice pronunciation, and improve fluently.
        </p>
        <div class="hero-buttons">
          <router-link to="/dashboard/youtube" class="btn-hero-primary">Start Learning</router-link>
          <router-link to="/transcribe?videoId=rtuKId76DZI" class="btn-hero-secondary">Watch Demo</router-link>
        </div>
        <div class="hero-stats">
          <div class="stat-item">
            <div class="stat-number">1000+</div>
            <div class="stat-label">Active Learners</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">10K+</div>
            <div class="stat-label">Video Lessons</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">99.9%</div>
            <div class="stat-label">Satisfaction Rate</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="features" id="features">
      <div class="container">
        <div class="section-header">
          <h2>Powerful Features at Your Fingertips</h2>
          <p>AI-driven learning tools to accelerate your English mastery</p>
        </div>
        <div class="features-grid">
          <div class="feature-card" v-for="feature in features" :key="feature.icon">
            <div class="feature-icon">{{ feature.icon }}</div>
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.description }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Testimonials Section -->
    <section class="testimonials" id="testimonials">
      <div class="container">
        <div class="section-header">
          <h2>What Our Students Say</h2>
        </div>
        <div class="testimonials-grid">
          <div class="testimonial-card" v-for="testimonial in testimonials" :key="testimonial.name">
            <div class="testimonial-content">
              <p>{{ testimonial.content }}</p>
            </div>
            <div class="testimonial-author">
              <div class="author-avatar">{{ testimonial.avatar }}</div>
              <div class="author-info">
                <div class="author-name">{{ testimonial.name }}</div>
                <div class="author-title">{{ testimonial.title }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Pricing Section -->
    <section class="pricing" id="pricing">
      <div class="container">
        <div class="section-header">
          <h2>Choose Your Learning Plan</h2>
          <p>All plans include core features, upgrade anytime</p>
        </div>
        <div class="pricing-grid">
          <div class="pricing-card" v-for="plan in pricingPlans" :key="plan.name" :class="{ 'pricing-featured': plan.featured }">
            <div v-if="plan.featured" class="featured-badge">Popular</div>
            <div class="pricing-header">
              <h3>{{ plan.name }}</h3>
              <div class="price">
                <template v-if="plan.price !== null">
                  <span class="price-currency">$</span>
                  <span class="price-amount">{{ plan.price }}</span>
                  <span class="price-period">/month</span>
                </template>
                <span v-else class="price-text">Contact Us</span>
              </div>
            </div>
            <ul class="pricing-features">
              <li v-for="feature in plan.features" :key="feature">✓ {{ feature }}</li>
            </ul>
            <router-link v-if="plan.link" :to="plan.link" class="pricing-btn">{{ plan.buttonText }}</router-link>
            <a v-else href="#contact" class="pricing-btn">{{ plan.buttonText }}</a>
          </div>
        </div>
      </div>
    </section>

    <!-- How It Works Section -->
    <section class="how-it-works" id="how-it-works">
      <div class="container">
        <div class="section-header">
          <h2>How It Works</h2>
          <p>Start learning English in 3 simple steps</p>
        </div>
        <div class="steps-grid">
          <div class="step-card" v-for="(step, index) in howItWorks" :key="index">
            <div class="step-number">{{ index + 1 }}</div>
            <div class="step-icon">{{ step.icon }}</div>
            <h3>{{ step.title }}</h3>
            <p>{{ step.description }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- FAQ Section -->
    <section class="faq" id="faq">
      <div class="container">
        <div class="section-header">
          <h2>Frequently Asked Questions</h2>
          <p>Everything you need to know about X Learning</p>
        </div>
        <div class="faq-list">
          <div class="faq-item" v-for="(item, index) in faqs" :key="index">
            <button class="faq-question" @click="toggleFaq(index)" :aria-expanded="openFaq === index">
              <span>{{ item.question }}</span>
              <span class="faq-toggle">{{ openFaq === index ? '−' : '+' }}</span>
            </button>
            <div class="faq-answer" v-show="openFaq === index">
              <p>{{ item.answer }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="cta">
      <div class="container">
        <div class="cta-content">
          <h2>Ready to Transform Your English?</h2>
          <p>Join thousands of learners improving their skills every day</p>
          <div class="cta-buttons">
            <router-link to="/register" class="btn-cta-primary">Start Free</router-link>
            <router-link to="/login" class="btn-cta-secondary">Already have an account? Sign In</router-link>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <div class="container">
        <div class="footer-grid">
          <div class="footer-section">
            <div class="footer-logo">
              <div class="logo-icon">
                <svg viewBox="0 0 90 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <rect width="90" height="20" rx="2" fill="#FF0000"/>
                  <text x="45" y="14" font-family="Arial, sans-serif" font-size="12" font-weight="bold" fill="white" text-anchor="middle">EE</text>
                </svg>
              </div>
              <span class="logo-text">X Learning</span>
            </div>
            <p class="footer-description">AI-powered English learning platform for everyone</p>
          </div>
          <div class="footer-section">
            <h4>Product</h4>
            <ul>
              <li><a href="#features">Features</a></li>
              <li><a href="#solutions">Solutions</a></li>
              <li><a href="#pricing">Pricing</a></li>
              <li><a href="#">Updates</a></li>
            </ul>
          </div>
          <div class="footer-section">
            <h4>Resources</h4>
            <ul>
              <li><a href="#">Help Center</a></li>
              <li><a href="#">Documentation</a></li>
              <li><a href="#">Video Tutorials</a></li>
              <li><a href="#">API Docs</a></li>
            </ul>
          </div>
          <div class="footer-section">
            <h4>Company</h4>
            <ul>
              <li><a href="#">About Us</a></li>
              <li><a href="#">Contact</a></li>
              <li><a href="#">Privacy Policy</a></li>
              <li><a href="#">Terms of Service</a></li>
            </ul>
          </div>
        </div>
        <div class="footer-bottom">
          <p>&copy; 2025 X Learning. All rights reserved.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const mobileMenuOpen = ref(false)
const openFaq = ref(null)

const toggleFaq = (index) => {
  openFaq.value = openFaq.value === index ? null : index
}

const howItWorks = [
  {
    icon: '🔗',
    title: 'Paste a YouTube Link',
    description: 'Copy any YouTube video URL and paste it into X Learning. We support videos in any language with English content.'
  },
  {
    icon: '⚡',
    title: 'AI Generates Subtitles',
    description: 'Our AI instantly transcribes the video and creates interactive subtitles with translations and word definitions.'
  },
  {
    icon: '🎯',
    title: 'Practice & Improve',
    description: 'Use shadowing mode to practice pronunciation, repeat sentences, and track your progress over time.'
  }
]

const faqs = [
  {
    question: 'What is X Learning?',
    answer: 'X Learning is an AI-powered English learning platform that helps you learn from YouTube videos. You can import any video, get AI-generated subtitles, practice pronunciation with shadowing, and track your progress.'
  },
  {
    question: 'Is X Learning free to use?',
    answer: 'Yes! X Learning offers a free Basic plan that includes 5 videos per month, basic pronunciation feedback, and standard subtitles. For unlimited access and advanced features, you can upgrade to our Pro plan.'
  },
  {
    question: 'How does the shadowing feature work?',
    answer: 'Shadowing is a language learning technique where you listen to a sentence and immediately repeat it. X Learning plays each sentence, records your voice, and uses AI to analyze your pronunciation, giving you instant feedback on accuracy and fluency.'
  },
  {
    question: 'Can I use X Learning on my phone?',
    answer: 'Absolutely! X Learning is fully responsive and works on desktop, tablet, and mobile devices. You can learn anywhere, anytime, on any device with an internet connection.'
  },
  {
    question: 'What types of YouTube videos work best?',
    answer: 'Any YouTube video with clear English speech works great. Popular choices include TED Talks, interviews, podcasts, movie clips, and educational content. Videos with one speaker and minimal background noise give the best results.'
  }
]

const features = [
  {
    icon: '🎥',
    title: 'YouTube Video Learning',
    description: 'Learn from authentic YouTube content. Import any video and generate interactive learning materials instantly.'
  },
  {
    icon: '🎙️',
    title: 'AI Pronunciation Practice',
    description: 'AI-powered speech recognition to practice and perfect your pronunciation with instant feedback.'
  },
  {
    icon: '📝',
    title: 'Smart Subtitles',
    description: 'Interactive subtitles with translations, definitions, and context to help you understand every word.'
  },
  {
    icon: '🔒',
    title: 'Secure & Private',
    description: 'Enterprise-grade security with multiple authentication layers. Your learning data is safe with us.'
  },
  {
    icon: '📱',
    title: 'Cross-Platform',
    description: 'Perfect support for desktop and mobile devices. Learn anywhere, anytime, on any device.'
  }
]

const testimonials = [
  {
    content: 'X Learning completely transformed my learning approach. The YouTube integration is brilliant – I can learn from content I actually enjoy watching. My speaking confidence has doubled in just 3 months!',
    avatar: '👨‍💼',
    name: 'David Chen',
    title: 'Software Engineer'
  },
  {
    content: 'As a beginner, this platform made learning English so much easier. The AI pronunciation feedback is incredibly helpful. I can now have basic conversations with confidence!',
    avatar: '👩‍💻',
    name: 'Maria Garcia',
    title: 'Marketing Professional'
  },
  {
    content: 'Our whole family uses this platform for learning. The interface is clean and intuitive, and the video library keeps growing. It\'s the best investment we\'ve made in education.',
    avatar: '👨‍👩‍👧',
    name: 'The Johnson Family',
    title: 'Family Plan Users'
  }
]

const pricingPlans = [
  {
    name: 'Basic',
    price: 0,
    features: ['5 videos per month', 'Basic pronunciation feedback', 'Standard subtitles', 'Mobile access', 'Community support'],
    buttonText: 'Start Free',
    link: '/register'
  },
  {
    name: 'Pro',
    price: 19,
    featured: true,
    features: ['Unlimited videos', 'Advanced AI feedback', 'Smart subtitles with translations', 'Offline mode', 'Progress tracking', 'Priority support'],
    buttonText: 'Upgrade Now',
    link: '/register'
  },
  {
    name: 'Enterprise',
    price: null,
    features: ['All Pro features', 'Team management', 'Custom content', 'Dedicated account manager', 'API access', 'Private deployment'],
    buttonText: 'Contact Sales',
    link: null
  }
]

const scrollTo = (id) => {
  const element = document.getElementById(id)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth' })
  }
  mobileMenuOpen.value = false
}
</script>

<style scoped src="@/assets/styles/landing.css"></style>


