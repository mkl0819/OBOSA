<template>
  <div class="item-list">
  <v-layout row wrap pa-5>
    <!-- <v-flex v-for="i in products.length > limits ? limits : products.length" xs12 sm6 md4 lg3>
      <Product
      :pId="products[i-1].pid"
      :pName="products[i-1].pname"
      :pDesc="products[i-1].pdescription"
      :prodImgs="products[i-1].productImgs"
      ></Product> -->
      <v-flex v-for="product in products" xs12 sm6 md4 lg3 v-bind:key="product.pid">
        <Product :product="product"></Product>
      <v-divider></v-divider>
    </v-flex>
    <!-- <v-flex xs12 text-xs-center round my-5 v-if="loadMore">
      <v-btn color="info" dark v-on:click="loadMorePosts"><v-icon size="25" class="mr-2">fa-plus</v-icon> 더 보기</v-btn>
    </v-flex> -->
  </v-layout>
  <productRegister></productRegister>
</div>
</template>
<script>
import { mapActions } from "vuex";
import { mapGetters } from "vuex";
import { mapState } from "vuex";
import productRegister from "../product/productRegister"
import Product from "../product/product"
export default {
  props: {
    column: {type: Number, default: 1},
    limits: {type: Number, default: 4},
    loadMore: {type: Boolean, default: true}
  },
  data() {
    return {
      products: [],
      dialog: false
    }
  },
  watch: {
    productList: function() {
      this.setProductList()
      console.log("error")
    }
  },
  computed: {
    ...mapState('productModule', ['productList'])
  },
  components: {
    Product,
    productRegister,
  },
  async beforeMount() {
    await this.getProductList()
    await this.setProductList()
  },
  methods: {
    ...mapActions('productModule', ['readProduct']),
    ...mapGetters('productModule', ['getProductState']),
    // productCRUD
    getProductList(){
      this.readProduct()
    },
    async setProductList(){
      console.log("set productlist");
      this.products = await this.getProductState()
    },
  }
}
</script>
<style>
.mw-700 {
  max-width: 700px;
  margin: auto;
}
</style>
