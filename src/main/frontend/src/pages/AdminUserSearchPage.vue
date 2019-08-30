<template>
  <div
    v-if="profile && profile.admin"
    class="layout-admin-user-search-page">
    <h1 class="h2">{{ $t('title.admin.page') }}</h1>
    <div class="user-search">
      <input
        v-model="username"
        :placeholder="$t('placeholder.searchUser')"
        @keyup.enter="goToUserPage" >
      <button
        class="btn btn--primary btn--outline"
        @click.prevent="goToUserPage">
        {{ $t('btn.search') }}
      </button>
    </div>
    <div class="session-container">
      <div
        v-if="!session"
        class="not-found-container">
        <div>
          <img
            src="~@/assets/images/icon-error.svg"
            alt="Error icon">
        </div>
        <h1 class="h3">Ainda não tem uma sessão aberta</h1>
      </div>
      <div v-else>
        <h1 class="h3">Sessão Iniciada</h1>
        <div class="session-info-container">
          <p>Criada Em: {{ session.createdAt }}</p>
          <p>Host: {{ session.ipAddress }}</p>
        </div>
        <div
          v-if="!session.userMifare"
          class="loading-bar">
          <div class="blue-bar" />
        </div>
        <div
          v-else
          class="session-user-container">
          <div v-if="session.userIstId">
            <h1 class="h4">Mifare encontrado</h1>
            <img
              :src="userPhotoUrl"
              alt="User Photo" >
            <p>{{ session.userIstId }}</p>
            <p>{{ session.userMifare }}</p>
          </div>
          <div
            v-else
            class="user-not-found-container">
            <h1 class="h4">Mifare não encontrado</h1>
            <div class="user-not-found">
              {{ session.userMifare }}
              <input
                v-model="deliverUsername"
                placeholder="Introduzir username"
                @keyup.enter="goToUserPage" >
              <button
                class="btn btn--primary btn--outline"
                @click.prevent="goToUserPage">
                Submeter
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <UnauthorizedPage v-else />
</template>

<script>
import CardsAPI from '@/api/cards'
import UnauthorizedPage from '@/pages/UnauthorizedPage'
import { mapState } from 'vuex'

export default {
  name: 'AdminUserSearchPage',
  components: {
    UnauthorizedPage
  },
  data () {
    return {
      username: '',
      deliverUsername: '',
      getAdminSessionInterval: undefined,
      session: undefined
    }
  },
  computed: {
    ...mapState([
      'profile'
    ]),
    userPhotoUrl () {
      return this.session && `data:image/png;base64,${this.session.userPhoto}`
    }
  },
  created () {
    this.getAdminSession()
    this.getAdminSessionInterval = setInterval(() => this.getAdminSession(), 1000)
  },
  destroyed () {
    clearInterval(this.getAdminSessionInterval)
  },
  methods: {
    goToUserPage () {
      const lowerUsername = this.username.toLowerCase()

      if (lowerUsername && lowerUsername !== this.profile.username) {
        this.$router.push({ name: 'AdminViewUserCardsPage', params: { username: lowerUsername } })
      } else {
        this.$router.push({ name: 'ListCardsPage' })
      }
    },
    async getAdminSession () {
      try {
        this.session = await CardsAPI.getAdminSession()
      } catch (err) {
        this.session = undefined
      }
    }
  }
}
</script>

<style lang="scss">
  .layout-admin-user-search-page {
    margin: 5rem 0 0;
    max-width: 71.25rem;
    display: flex;
    flex-flow: column nowrap;
    align-items: center;
    position: relative;
    justify-content: stretch;
    flex-grow: 1;
    overflow-x: hidden;
  }

  .user-search {
    display: flex;
    & > input {
      margin-right: 10px;
      padding-left: 10px;
    }
  }

  .session-container {
    margin-top: 8rem;
    width: 80%;

    .not-found-container {
      text-align: center;
    }

    & h1 {
      text-align: center;
    }

    .session-info-container {
      display: flex;
      text-align: left;
      flex-direction: column;
      justify-content: start;
    }

    .session-user-container {
      text-align: center;

      & img {
        width: 100px;
      }

      &  .user-not-found-container {
        margin: 0 auto;
        width: 30rem;
      }

      & .user-not-found {
        display: flex;
        align-items: center;
        justify-content: space-between;
      }
    }
  }
</style>
