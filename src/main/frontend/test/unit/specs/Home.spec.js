import Vue from 'vue'
import Home from '@/components/Home'

describe('Home.vue', () => {
  it('should render correct contents', () => {
    const Constructor = Vue.extend(Home)
    const vm = new Constructor().$mount()
    console.log(vm.$el.querySelector('h2'))
    expect(vm.$el.querySelector('h2').textContent)
      .to.equal('Home')
  })
})