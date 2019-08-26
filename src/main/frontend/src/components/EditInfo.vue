<template>
  <modal
    v-scroll-lock="open"
    :withfooter="true"
    :value="open"
    class="modal--lg"
    @input="$emit('close')">
    <template slot="modal-panel">
      <div>
        <loading v-if="hasPendingRequest" />
        <div
          v-if="!hasPendingRequest"
          class="form">
          <h1 class="h2">Is everything right?</h1>
          <div class="f-group">
            <p>Photo</p>
            <p class="small f-field--readonly">Your photo is managed in your <a
              class="u-active-link"
              href="https://fenix.tecnico.ulisboa.pt/">Fenix personal area</a>. In case you want
              to <a
                class="u-active-link"
                href="https://fenix.tecnico.ulisboa.pt/">change it</a>, please mind that it will be pending approval to figure on your card.
            </p>
          </div>
          <div class="f-group">
            <div>
              <p>Name</p>
              <a
                class="u-active-link small"
                @click.prevent="resetNames">Reset</a>
            </div>
            <tag-input
              :tags="userNamesList"
              :class="{ 'f-field--danger': userNameExceedsLength }"
              @remove-tag="removeUserName"/>
            <p
              v-if="userNameExceedsLength"
              class="small f-field__validation danger">
              Please shorten your displayed name (until 40 chars including spaces).
            </p>
            <p
              v-else
              class="small f-field__validation">If you want to shorten your displayed name,
              choose at least one of your first and last names.
            </p>
          </div>
        </div>
      </div>
    </template>
    <template
      slot="modal-footer">
      <div class="btn--group layout-list-cards__modal-footer">
        <button
          :class="{ 'btn--disabled': hasPendingRequest}"
          class="btn btn--light"
          @click.prevent="$emit('close')">
          {{ $t('btn.cancel') }}
        </button>
        <button
          :class="{ 'btn--disabled': userNameExceedsLength || hasPendingRequest}"
          class="btn btn--primary"
          @click.prevent="submitCardName">
          {{ $t('btn.confirm') }}
        </button>
      </div>
    </template>
  </modal>
</template>

<script>
import { mapActions } from 'vuex'
import CardsAPI from '@/api/cards'
import Loading from '@/components/Loading'
import Modal from '@/components/utils/Modal'
import TagInput from '@/components/utils/TagInput'

export default {
  name: 'EditInfo',
  components: {
    TagInput,
    Loading,
    Modal
  },
  props: {
    open: {
      type: Boolean,
      required: true
    }
  },
  data () {
    return {
      hasPendingRequest: false,
      fullName: undefined,
      chosenUserNames: { givenNames: [], familyNames: [] },
      exludedNames: ['da', 'das', 'do', 'dos', 'de', 'e']
    }
  },
  computed: {
    userNameExceedsLength () {
      const nameLength = this.userNamesList.map(name => name.value).join(' ').length
      return nameLength > 40
    },
    userNamesList () {
      const { givenNames, familyNames } = this.chosenUserNames
      return [...givenNames, ...familyNames]
    }
  },
  watch: {
    open: {
      immediate: true,
      handler (open) {
        if (open) {
          if (!this.fullName) {
            this.fetchUserNames()
          } else {
            this.resetNames()
          }
        }
      }
    }
  },
  methods: {
    ...mapActions([
      'fetchPreview'
    ]),
    async fetchUserNames () {
      this.hasPendingRequest = true
      const response = await CardsAPI.getUserNames()
      this.fullName = response
      this.resetNames()
      this.hasPendingRequest = false
    },
    async submitCardName () {
      this.hasPendingRequest = true
      await CardsAPI.changeCardName(this.userNamesList.map(name => name.value).join(' '))
      this.fetchPreview()
      this.hasPendingRequest = false
      this.$emit('close')
    },
    removeUserName (name, index) {
      const { givenNames, familyNames } = this.chosenUserNames
      const numGivenNames = givenNames.length
      if (index <= numGivenNames - 1) {
        givenNames.splice(index, 1)

        const validGivenNames = givenNames.filter(name => !this.exludedNames.includes(name.value))
        if (validGivenNames.length === 1) {
          givenNames.find(name => name.value === validGivenNames[0].value).disableRemoveAction = true
        }
      } else {
        familyNames.splice(index - numGivenNames, 1)
        const validFamilyNames = familyNames.filter(name => !this.exludedNames.includes(name.value))
        if (validFamilyNames.length === 1) {
          familyNames.find(name => name.value === validFamilyNames[0].value).disableRemoveAction = true
        }
      }
    },
    resetNames () {
      const givenNamesList = this.fullName.givenNames.split(' ')
      const validGivenNames = givenNamesList.filter(name => !this.exludedNames.includes(name.value))
      const familyNamesList = this.fullName.familyNames.split(' ')
      const validFamilyNames = familyNamesList.filter(name => !this.exludedNames.includes(name.value))
      this.chosenUserNames = {
        givenNames: givenNamesList.map(n => ({ value: n, disableRemoveAction: validGivenNames.length === 1 })),
        familyNames: familyNamesList.map(n => ({ value: n, disableRemoveAction: validFamilyNames.length === 1 }))
      }
    }
  }
}
</script>

<style lang="scss">
// import variables
@import "@/assets/scss/_variables.scss";

.f-group{
  text-align: left;

  .f-field--readonly {
    margin: 1rem 0;
  }

  &:last-child {
    p:first-child {
      float: left;
    }
    .u-active-link {
      float: right;
      margin-top: 5px;
      font-weight: 400;
    }
  }

  .f-tag-field {
    margin-top: 2.5rem;
  }

  .f-field--danger {
    border-color: $magenta;
    + .f-field__validation{
      color: $magenta;
    }
  }
}

</style>
